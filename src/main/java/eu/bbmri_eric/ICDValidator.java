package eu.bbmri_eric;

/**
 * A simple validator of ICD codes.
 */
public interface ICDValidator {
    /**
     * Check weather a given string is a valid ICD code
     * @param icd code
     * @return true if such an ICD code exists
     */
    boolean isValid(String icd);
}
