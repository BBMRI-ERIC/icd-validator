[![CI/CD pipeline](https://github.com/BBMRI-ERIC/icd-validator/actions/workflows/ci.yml/badge.svg)](https://github.com/BBMRI-ERIC/icd-validator/actions/workflows/ci.yml)
# ICD Validator
A simple Java library for validating ICD codes.
## Status
Currently only supports ICD-10 codes
## Usage

```java
ICDValidator validator = new ICD10Validator();
validator.isValid("B18.0");
```