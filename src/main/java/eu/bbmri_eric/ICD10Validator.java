package eu.bbmri_eric;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class ICD10Validator implements ICDValidator {
    private static Set<String> validCodes = null;
    private static Set<Pattern> codePatterns = null;
    private static boolean initialized = false;
    private static final Object lock = new Object();

    @Override
    public boolean isValid(String icd) {
        if (icd == null || icd.trim().isEmpty()) {
            return false;
        }

        if (!initialized) {
            synchronized (lock) {
                if (!initialized) {
                    initializeCodes();
                    initialized = true;
                }
            }
        }

        icd = icd.trim().toUpperCase();

        if (validCodes.contains(icd)) {
            return true;
        }

        for (Pattern pattern : codePatterns) {
            if (pattern.matcher(icd).matches()) {
                return true;
            }
        }

        return false;
    }

    static void resetForTesting() {
        synchronized (lock) {
            validCodes = null;
            codePatterns = null;
            initialized = false;
        }
    }

    static Set<String> getValidCodesForTesting() {
        return validCodes;
    }

    private static void initializeCodes() {
        validCodes = new HashSet<>(10000);
        codePatterns = new HashSet<>(1000);

        try (InputStream is = ICD10Validator.class.getClassLoader().getResourceAsStream("icd102019en.xml")) {
            if (is == null) {
                throw new IllegalStateException("ICD XML file not found in resources");
            }

            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(new InputSource(is), new ICDCodeHandler());

            addSubdivisionPatterns();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize ICD validator", e);
        }
    }

    private static class ICDCodeHandler extends DefaultHandler {
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if ("Class".equalsIgnoreCase(qName)) {
                String code = attributes.getValue("code");
                if (code != null && !code.trim().isEmpty()) {
                    processCode(code.trim());
                }
            }
        }

        private void processCode(String code) {
            if (code.contains("-")) {
                addCodeRange(code);
            } else if (code.matches("^[A-Z]\\d{2}(\\.\\d{1})?$")) {
                validCodes.add(code);
            }
        }
    }

    private static void addCodeRange(String range) {
        String[] parts = range.split("-");
        if (parts.length != 2 || !parts[0].matches("^[A-Z]\\d{2}$") || !parts[1].matches("^[A-Z]\\d{2}$")) {
            return;
        }

        char letter = parts[0].charAt(0);
        if (letter != parts[1].charAt(0)) {
            return;
        }

        try {
            int start = Integer.parseInt(parts[0].substring(1));
            int end = Integer.parseInt(parts[1].substring(1));
            for (int i = start; i <= end; i++) {
                String code = String.format("%c%02d", letter, i);
                validCodes.add(code);
            }
        } catch (NumberFormatException e) {
            // Ignore invalid ranges
        }
    }

    private static void addSubdivisionPatterns() {
        String[] fourthCharRanges = {
                "E10-E14:[0-9]", "F10-F19:[0-9]", "F70-F79:[0-3]", "K25-K28:[0-8]",
                "N00-N07:[0-9]", "O03-O06:[0-9]", "R83-R89:[0-9]",
                "V01-V06:[0-2]", "V10-V18:[0-6]", "V20-V28:[0-6]", "V30-V38:[0-6]",
                "V40-V48:[0-6]", "V50-V58:[0-6]", "V60-V68:[0-6]", "V70-V78:[0-6]",
                "V90-V94:[0-8]", "Y70-Y82:[0-8]"
        };

        for (String range : fourthCharRanges) {
            String[] parts = range.split(":");
            String[] codes = parts[0].split("-");
            String pattern = parts[1];

            char letter = codes[0].charAt(0);
            int start = Integer.parseInt(codes[0].substring(1));
            int end = codes.length > 1 ? Integer.parseInt(codes[1].substring(1)) : start;

            for (int i = start; i <= end; i++) {
                String baseCode = String.format("%c%02d", letter, i);
                codePatterns.add(Pattern.compile("^" + Pattern.quote(baseCode) + "\\." + pattern + "$"));
            }
        }

        String[] fifthCharRanges = {"B18.0-B18.1:[0-2]", "J96:[0-2]"};
        for (String range : fifthCharRanges) {
            String[] parts = range.split(":");
            String[] codes = parts[0].split("-");
            String pattern = parts[1];

            char letter = codes[0].charAt(0);
            int start = Integer.parseInt(codes[0].replaceAll("[^0-9]", ""));
            int end = codes.length > 1 ? Integer.parseInt(codes[1].replaceAll("[^0-9]", "")) : start;

            for (int i = start; i <= end; i++) {
                String baseCode = String.format("%c%02d", letter, i);
                codePatterns.add(Pattern.compile("^" + Pattern.quote(baseCode) + "\\.[0-9][" + pattern + "]$"));
            }
        }
    }
}