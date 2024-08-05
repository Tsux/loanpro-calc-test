package org.calculator;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class TestCalculator {
    private final static String COMMAND_BASE = "docker run --rm public.ecr.aws/l4q9w4c5/loanpro-calculator-cli";
    private final static String COMMAND = COMMAND_BASE + " %s %.8f %.8f";
    private final static String COMMAND_2 = COMMAND_BASE + " %s %s %.8f";
    private final static String COMMAND_3 = COMMAND_BASE + " %s %s %s";

    private final static String usageDirections =
            "Usage: cli-calculator operation operand1 operand2\n" +
            "Supported operations: add, subtract, multiply, divide\n";
    private final static String unknownOperationMessage = "Error: Unknown operation: division\n";
    private final static String invalidOperandMessage = "Invalid argument. Must be a numeric value.\n";
    private final static String divisionByZeroMessage = "Error: Cannot divide by zero\n";

    @DataProvider(name = "regularOperations")
    public Object[][] regularOperations() {
        return new Object[][] {
                { "add",        999,    5,      "1004"  },
                { "subtract",   1599,   497,    "1102"  },
                { "subtract",   129,    130,    "-1"    },
                { "multiply",   25,     15,     "375"   },
                { "multiply",   35,     0,      "0"   },
                { "divide",     75,     5,      "15"    },
        };
    }

    @DataProvider(name = "operationFloatNumbers")
    public Object[][] operationFloatNumbers() {
        return new Object[][] {
                // TODO: This is failing cause int numbers are adding up but floatings are being subtracted
                { "add",        2147483645.0000001,     1.0000008,      "2147483646.0000009"    },
                { "subtract",   2147483645.0000001,     50.0000008,     "2147483594.9999993"    },
                { "multiply",   333.0000002,            12.0000009,     "3996.0003021"          },
                { "multiply",   333.0000002,            12.0009999,     "3996.3329691"          },
                { "multiply",   333.0000102013,         12.00000099,    "3996.3329691"          },
                { "divide",     21.0000002,             78.0000003,     "0.26923077"            },
                { "divide",     100,                    33,             "3.03030303"            }
        };
    }

    @DataProvider(name = "operationNegativeOps")
    public Object[][] operationNegativeOps() {
        return new Object[][] {
                { "add",        -12,                    -213,           "-225"                  },
                { "add",        2147483645.0000001,     -1.0000008,     "2147483643.9999993"    },
                { "subtract",   -21789,                 -4999,          "-16790"                },
                { "subtract",   100,                    -20,            "120"                   },
                { "subtract",   -2147483645.0000001,    -1.0000008,     "-2147483643.9999993"   },
                // TODO: This is failing cause int numbers are adding up but floatings are being subtracted
                { "subtract",   -2147483645.0000001,    1.0000008,      "-2147483646.0000009"   },
                { "multiply",   -30,                    -99,            "2970"                  },
                { "multiply",   -111,                   35,             "-3885"                 },
                { "multiply",   127,                    16909320.0551,  "2147483646.9977002"    },
                { "divide",     999,                    -33,            "-30.27272727"          },
                { "divide",     -999,                   -33,            "30.27272727"           }
        };
    }

    @DataProvider(name = "expectedRoundings")
    public Object[][] expectedRoundings() {
        return new Object[][] {
                { "add",        12.00000000000099,     2.000000000033,  "14"    },
                { "subtract",   12.00000000000099,     2.000000000033,  "10"    },
                // TODO: Multiply rounding scenario is failing on an intermittent basis
                { "multiply",   12.00000000000099,      2.000000000033, "24"    },
                { "divide",     12.00000000000099,      2.000000000033, "6"     }
        };
    }

    @DataProvider(name = "boundaryValueAnalysis")
    public Object[][] boundaryValueAnalysis() {
        return new Object[][] {
                // Checks biggest integer value is computed through float
                { "add",        2147483647,     1,          "2147483648" },
                // Checks outcome from trying to compute a sum which expected outcome is beyond float's biggest number
                // TODO: This is failing because addition can't be applied to already biggest float number (1.7e+308),
                //  suggestion is to add an error message instead of wrong computation result (omitted addition)
                { "add",        1.7e+308,       1,          "170000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001" },
                // Checks lowest integer value is computed through float
                { "subtract",   -2147483647,    1,          "-2147483648" },
                // Checks outcome from trying to compute a sum which expected outcome is beyond float's lowest number
                // TODO: This is failing because subtraction can't be applied to already lowest float number (1.7e+308),
                //  suggestion is to add an error message instead of wrong computation result (omitted subtraction)
                { "subtract",   -1.7e308,       1,          "-170000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001" },
                // Checks outcome from product with too big operand
                { "multiply",   1.0625e+306,    12,         "12749999999999999000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" },
                // Checks outcome from product which result is expected to be beyond float biggest number
                // TODO: Confirm that infinite symbol is an expected outcome for such an operation
                { "multiply",   1.7e+307,       12,         "∞" },
                // FIXME-01: below test sample fails out of division by 0, this comes from wrong conversion 1.2e-108
                //  created TC edgeCaseDivisionBySmallNumber
                // Checks outcome from division with too small operands
                // { "divide",     1.7e-308,       1.2e-108,   "0" }
        };
    }

    @DataProvider(name = "operationImaginaryNumbers")
    public Object[][] operationImaginaryNumbers() {
        return new Object[][] {
                // TODO: This is failing cause int numbers are adding up but floatings are being subtracted
                { "add",        "-4i",     4,   "Invalid argument. Must be a numeric value.\n"    },
                { "subtract",   "-7i",     2,   "Invalid argument. Must be a numeric value.\n"    },
                { "multiply",   "-11i",    12,  "Invalid argument. Must be a numeric value.\n"    },
                { "divide",     "-110i",   11,  "Invalid argument. Must be a numeric value.\n"    }
        };
    }

    @Test(description = "C018 [Bad Path]")
    public void testInvokeWithNoArguments() {
        String res = getCalculatorOut("", 0, 0);

        Assert.assertEquals(res, usageDirections);
    }

    @Test(description = "C019 [Bad Path]")
    public void testInvocationNotSufficingArguments() {
        String res = getCalculatorOut("multiply", "999", "");

        Assert.assertEquals(res, usageDirections);
    }

    @Test(description = "C020 [Bad Path]")
    public void testUnknownOperation() {
        String res = getCalculatorOut("division", "17", "7");

        Assert.assertEquals(res, unknownOperationMessage);
    }

    @Test(description = "C021 [Bad Path]")
    public void testInvalidOperand() {
        String res = getCalculatorOut("division", "hola", "2");

        Assert.assertEquals(res, invalidOperandMessage);
    }

    @Test(description = "C022 [Bad Path]")
    public void testTooManyArguments() {
        String res = getCalculatorOut("multiply", "2", "3 4 10");

        // TODO: Current behavior is to ignore operands from third one. In sake of behavior accuracy, a good improvement
        //  would be to:
        //  a) Allow multiple operands for certain operations (e.g. add, multiply)
        //  b) Keep calculation behavior but add a warning message to notify the user that additional args  were ignored
        Assert.assertEquals(res, "6");
    }

    @Test(description = "C023 [Bad Path]")
    public void testDivisionByZero() {
        String res = getCalculatorOut("divide", "2", "0");

        Assert.assertEquals(res, divisionByZeroMessage);
    }

    @Test(description = "C012 [Bad Path]")
    public void testDivisionZeroDivider() {
        String res = getCalculatorOut("divide", "0", "16");

        Assert.assertEquals(res, "0");
    }

    @Test(description = "C001, C002, C003, C004", dataProvider = "regularOperations")
    public void testRegularOperations(String operation, double op1, double op2, String expectation) {
        String res = getCalculatorOut(operation, op1, op2);
        Assert.assertFalse(res.isEmpty());

        Assert.assertEquals(res, expectation);
    }

    @Test(description = "C005, C013, C014, C015", dataProvider = "operationNegativeOps")
    public void testOperationsNegativeNumbers(String operation, double op1, double op2, String expectation) {
        String res = getCalculatorOut(operation, op1, op2);
        Assert.assertFalse(res.isEmpty());

        Assert.assertEquals(res, expectation);
    }

    @Test(description = "C006, C008, C009", dataProvider = "operationFloatNumbers")
    public void testOperationsCommaFloatNumbers(String operation, double op1, double op2, String expectation) {
        String result = getCalculatorOut(operation, op1, op2);

        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(result, expectation);
    }

    @Test(description = "C007, C016, C017", dataProvider = "boundaryValueAnalysis")
    public void testOperationsInBoundaries(String operation, double op1, double op2, String expectation) {
        String res = getCalculatorOut(operation, op1, op2);
        Assert.assertFalse(res.isEmpty());

        Assert.assertEquals(res, expectation);
    }

    @Test(description = "C010", dataProvider = "expectedRoundings")
    protected void testValidateExpectedRoundings(String operation, double op1, double op2, String expectation) {
        String res = getCalculatorOut(operation, op1, op2);
        Assert.assertFalse(res.isEmpty());

        Assert.assertEquals(res, expectation);
    }

    // FIXME-01 workaround
    @Test(description = "C011 [Edge Case]")
    protected void testEdgeCaseDivisionBySmallNumber() {
        String res = getCalculatorOut("divide", "1.7e-308", "1.2e-108");
        Assert.assertFalse(res.isEmpty());

        Assert.assertEquals(res, "0");
    }

    @Test(description = "C024", dataProvider = "operationImaginaryNumbers")
    public void testOperationsImaginaryNumbers(String operation, String op1, double op2, String expectation) {
        String res = getCalculatorOut(operation, op1, op2);
        Assert.assertFalse(res.isEmpty());

        Assert.assertEquals(res, expectation);
    }

    public String getCalculatorOut(String operation, double operator1, double operator2) {
        String command = operation.isEmpty() ?
                COMMAND_BASE : String.format(COMMAND, operation, operator1, operator2);

        return runCommand(command);
    }

    public String getCalculatorOut(String operation, String operator1, double operator2) {
        String command = operation.isEmpty() ?
                COMMAND_BASE : String.format(COMMAND_2, operation, operator1, operator2);

        return runCommand(command);
    }

    public String getCalculatorOut(String operation, String operator1, String operator2) {
        String command = operation.isEmpty() ?
                COMMAND_BASE : String.format(COMMAND_3, operation, operator1, operator2);

        return runCommand(command);
    }

    public String runCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder builder = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(System.lineSeparator());
            }

            return  builder.toString().contains("Result") ?
                    builder.toString().replace("Result: ", "").replace("\n", "") : builder.toString();
        } catch (IOException ioException) {
            assert false : String.format(
                    "Test failed by below IO Exception: \n\tMessage: %s\n\tCause: %s",
                    ioException.getMessage(),
                    ioException.getCause());
        } catch (InterruptedException interruption) {
            assert false : String.format(
                    "Test failed due to a Process interruption. See details below: \n\tMessage: %s\n\tCause: %s",
                    interruption.getMessage(),
                    interruption.getCause());
        }

        return "";
    }
}
