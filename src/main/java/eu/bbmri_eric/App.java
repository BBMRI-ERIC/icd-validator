package eu.bbmri_eric;

public class App {
    public static void main(String[] args) {
        ICDValidator validator = new ICD10Validator();
        validator.isValid("B18.0");
    }
}
