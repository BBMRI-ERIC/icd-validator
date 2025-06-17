![Maven](https://img.shields.io/badge/apachemaven-C71A36.svg?style=for-the-badge&logo=apachemaven&logoColor=white)
[![CI/CD pipeline](https://github.com/BBMRI-ERIC/icd-validator/actions/workflows/ci.yml/badge.svg)](https://github.com/BBMRI-ERIC/icd-validator/actions/workflows/ci.yml)
# ICD Validator
A simple Java library for validating ICD codes.
## Status
Currently only supports validation of ICD-10 codes
## Installation
To use with Maven add the following dependency to your `pom.xml`
```xml
<dependency>
    <groupId>eu.bbmri-eric</groupId>
    <artifactId>icd-validator</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage

```java
ICDValidator validator = new ICD10Validator();
validator.isValid("B18.0");
```