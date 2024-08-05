# Intro
These tests covers LoanPro calculator App which is available thru Docker hub 

# Loan-Pro Specifications

Available operations are:

* add 
* subtract 
* multiply 
* divide

**Considerations and Known bugs:**
1. All commands take exactly two numbers. Attempting to use more or less
operands will result in an error message; this is expected behavior.
2. Dividing by zero returns an error message.
3. Results may be displayed as scientific notation. This is correct and
   expected, as long as the value is correct.
4. Results are guaranteed exact up to 8 decimal places. For example,
   adding 1.0000001 + 1.0000001 (six 0’s) yields the expected 2.0000002, but
   1.00000001 + 1.00000001 (seven 0’s) results in 2.0. Similar rounding errors
   due to the data type used are expected.
5. Operations resulting in infinity, negative infinity or “not a number” are
   supported.

# Download and Run the Application
First, you need to install docker if not yet running on your computer:
https://www.docker.com/products/docker-desktop/

Pull the image:
```
docker pull public.ecr.aws/l4q9w4c5/loanpro-calculator-cli:latest
```

After pulling the image, execute LoanPro-Calc by invoking:
```
docker run --rm public.ecr.aws/l4q9w4c5/loanpro-calculator-cli add 8 5
```

Execute the command without arguments for more usage information:
```
docker run --rm public.ecr.aws/l4q9w4c5/loanpro-calculator-cli
```
You can Test Loan-Pro by playing with above command

# Executing Tests
Since this is a test-only repository that's built with gradle, you can execute the tests by simply running:
* `./gradlew clean build` (For first build)
* `./gradlew test`(For subsequent test execution)

Alternatively, you can open this repository in your preferred IDE and use IDE's internal tools to execute the tests (e.g. IntelliJ)
