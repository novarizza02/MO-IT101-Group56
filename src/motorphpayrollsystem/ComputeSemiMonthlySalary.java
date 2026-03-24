package motorphpayrollsystem;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

/**
 * ComputeSemiMonthlySalary.java
 *
 * This class computes the semi-monthly salary of employees based on:
 * - selected month
 * - selected payroll cutoff
 * - attendance records
 * - hourly rate from the employee file
 *
 * Salary in this module is based on total hours worked only.
 * Government deductions are not applied here because they are handled
 * in the net pay module.
 *
 * Login Section:
 * The user must enter a valid username and password before accessing the feature.
 * Username: payroll_staff
 * Password: 12345
 *
 * If the login credentials are incorrect, access to the feature
 * will be denied and the user will return to the menu.
 */
public class ComputeSemiMonthlySalary {

    // Runs the semi-monthly salary feature.
    public static void run(Scanner input) {
        String employeeFilePath = MotorPHPayrollSystem.EMPLOYEE_FILE;
        String attendanceFilePath = MotorPHPayrollSystem.ATTENDANCE_FILE;

        System.out.println("==============================================================");
        System.out.println("              MOTORPH SEMI-MONTHLY SALARY REPORT              ");
        System.out.println("==============================================================");

        // Ask for the month to be processed.
        int selectedMonth = promptMonth(input);
        if (selectedMonth == 0) {
            return;
        }

        // Ask for the cutoff period to be processed.
        int selectedCutoff = promptCutoff(input);
        if (selectedCutoff == 0) {
            return;
        }

        // Ask whether to show all employees or only one employee.
        int reportViewOption = promptViewOption(input);
        if (reportViewOption == 0) {
            return;
        }

        int searchedEmployeeNumber = 0;

        // If only one employee is requested, ask for the employee number.
        if (reportViewOption == 2) {
            searchedEmployeeNumber = promptEmployeeNumber(input);
            if (searchedEmployeeNumber == 0) {
                return;
            }
        }

        int maxEmployees = MotorPHPayrollSystem.MAX_EMPLOYEES;

        // Parallel arrays used to store employee details and computed salary values.
        int[] employeeNumbers = new int[maxEmployees];
        String[] employeeNames = new String[maxEmployees];
        double[] hourlyRates = new double[maxEmployees];

        double[] totalHoursWorked = new double[maxEmployees];
        int[] totalLateMinutes = new int[maxEmployees];
        int[] daysWorked = new int[maxEmployees];
        double[] semiMonthlySalary = new double[maxEmployees];

        int employeeCount = 0;

        // Read employee details from the employee file.
        try (BufferedReader employeeReader = new BufferedReader(new FileReader(employeeFilePath))) {

            String employeeLine = employeeReader.readLine(); // Skip header row.

            while ((employeeLine = employeeReader.readLine()) != null) {

                // Remove quotation marks before splitting the CSV row.
                employeeLine = employeeLine.replace("\"", "");
                String[] employeeFields = employeeLine.split(",");

                // Skip incomplete rows.
                if (employeeFields.length < 3) {
                    continue;
                }

                int employeeNumber = Integer.parseInt(employeeFields[0].trim());
                String lastName = employeeFields[1].trim();
                String firstName = employeeFields[2].trim();
                double hourlyRate = Double.parseDouble(employeeFields[employeeFields.length - 1].trim());
                
                employeeNumbers[employeeCount] = employeeNumber;
                employeeNames[employeeCount] = firstName + " " + lastName;
                hourlyRates[employeeCount] = hourlyRate;

                employeeCount++;
            }

        } catch (IOException e) {
            System.out.println("Error reading employee file: " + e.getMessage());
            pressEnterToReturn(input);
            return;
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid number format found in employee file.");
            pressEnterToReturn(input);
            return;
        }

        // If one employee is requested, verify that the employee number exists.
        if (reportViewOption == 2) {
            int employeeIndex = findEmployeeIndex(employeeNumbers, employeeCount, searchedEmployeeNumber);

            if (employeeIndex == -1) {
                System.out.println("Employee number not found.");
                pressEnterToReturn(input);
                return;
            }
        }

        // Read attendance records and compute total worked hours per employee.
        try (BufferedReader attendanceReader = new BufferedReader(new FileReader(attendanceFilePath))) {
            String attendanceLine = attendanceReader.readLine(); // Skip header row.

            while ((attendanceLine = attendanceReader.readLine()) != null) {
                String[] attendanceFields = attendanceLine.split(",");

                // Skip incomplete rows.
                if (attendanceFields.length < 6) {
                    continue;
                }

                int employeeNumber = Integer.parseInt(attendanceFields[0].trim());
                String attendanceDate = attendanceFields[3].trim();
                String logInTime = attendanceFields[4].trim();
                String logOutTime = attendanceFields[5].trim();

                // If viewing one employee only, ignore other employees.
                if (reportViewOption == 2 && employeeNumber != searchedEmployeeNumber) {
                    continue;
                }

                String[] dateTokens = attendanceDate.split("/");
                if (dateTokens.length != 3) {
                    continue;
                }

                int attendanceMonth = Integer.parseInt(dateTokens[0]);
                int attendanceDay = Integer.parseInt(dateTokens[1]);
                int attendanceYear = Integer.parseInt(dateTokens[2]);

                // Accept only attendance records within the project dataset coverage.
                if (attendanceYear != 2024 || attendanceMonth < 6 || attendanceMonth > 12) {
                    continue;
                }

                // Process only the chosen month.
                if (attendanceMonth != selectedMonth) {
                    continue;
                }

                // Filter records based on the chosen cutoff.
                if (selectedCutoff == 1) {
                    if (attendanceDay < 1 || attendanceDay > 15) {
                        continue;
                    }
                } else {
                    if (attendanceDay < 16) {
                        continue;
                    }
                }

                int employeeIndex = findEmployeeIndex(employeeNumbers, employeeCount, employeeNumber);
                if (employeeIndex == -1) {
                    continue;
                }

                // get login minutes for lateness
                int loginMinutes = MotorPHPayrollSystem.convertTimeToMinutes(logInTime);

                // compute worked hours using shared method
                double workedHours = MotorPHPayrollSystem.computeWorkedHours(logInTime, logOutTime);

                // define official start for lateness
                int officialStart = 8 * 60;

                // compute late minutes for checking and validation purposes.
                int lateMinutes = 0;
                if (loginMinutes >= ((8 * 60) + 11)) {
                    lateMinutes = loginMinutes - officialStart;
                }

                totalHoursWorked[employeeIndex] += workedHours;
                totalLateMinutes[employeeIndex] += lateMinutes;
                daysWorked[employeeIndex]++;
            }

        } catch (IOException e) {
            System.out.println("Error reading attendance file: " + e.getMessage());
            pressEnterToReturn(input);
            return;
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid number format found in attendance file.");
            pressEnterToReturn(input);
            return;
        }

        // Compute semi-monthly salary using total hours worked multiplied by hourly rate.
        for (int employeeIndex = 0; employeeIndex < employeeCount; employeeIndex++) {
            semiMonthlySalary[employeeIndex] = totalHoursWorked[employeeIndex] * hourlyRates[employeeIndex];

            if (semiMonthlySalary[employeeIndex] < 0) {
                semiMonthlySalary[employeeIndex] = 0;
            }
        }

        // Display the report header and selected filter details.
        System.out.println();
        System.out.println("==============================================================");
        System.out.println("              MOTORPH SEMI-MONTHLY SALARY REPORT              ");
        System.out.println("==============================================================");
        System.out.println("Selected Month        : " + getMonthName(selectedMonth));
        System.out.println("Selected Cutoff       : " + getCutoffName(selectedCutoff));
        System.out.println("Coverage              : " + getCutoffCoverage(selectedCutoff));
        System.out.println("Work Hours Counted    : 8:00 AM to 5:00 PM only");
        System.out.println("Break Deduction       : 1 hour per workday");
        System.out.println("Grace Period Applied  : 8:00 AM to 8:10 AM");
        System.out.println("Late Rule Applied     : 8:11 AM onwards");

        if (reportViewOption == 1) {
            System.out.println("Report View           : All Employees");
        } else {
            System.out.println("Report View           : One Employee Only");
            System.out.println("Employee Number       : " + searchedEmployeeNumber);
        }

        System.out.println("==============================================================");

        boolean hasRecord = false;

        // Show the salary result for each matching employee.
        for (int employeeIndex = 0; employeeIndex < employeeCount; employeeIndex++) {
            if (reportViewOption == 2 && employeeNumbers[employeeIndex] != searchedEmployeeNumber) {
                continue;
            }

            if (daysWorked[employeeIndex] > 0) {
                hasRecord = true;

                System.out.println();
                System.out.println("Employee Number       : " + employeeNumbers[employeeIndex]);
                System.out.println("Employee Name         : " + employeeNames[employeeIndex]);
                System.out.println("Hourly Rate           : " + hourlyRates[employeeIndex]);
                System.out.println("Days Worked           : " + daysWorked[employeeIndex]);
                System.out.println("Total Hours Worked    : " + totalHoursWorked[employeeIndex]);
                System.out.println("Semi-Monthly Salary   : " + semiMonthlySalary[employeeIndex]);
                System.out.println("--------------------------------------------------------------");
            }
        }

        if (!hasRecord) {
            System.out.println();
            System.out.println("No attendance records found for the selected payroll period.");
        }

        // Basic validation checks to confirm that the computations are reasonable.
        // Validate the computed results by checking for invalid values,
        // unrealistic total hours worked, and incorrect salary computation.
        boolean computationCorrect = true;

        for (int employeeIndex = 0; employeeIndex < employeeCount; employeeIndex++) {
            if (reportViewOption == 2 && employeeNumbers[employeeIndex] != searchedEmployeeNumber) {
                continue;
            }

            if (totalHoursWorked[employeeIndex] < 0) {
                computationCorrect = false;
            }

            if (totalLateMinutes[employeeIndex] < 0) {
                computationCorrect = false;
            }

            if (semiMonthlySalary[employeeIndex] < 0) {
                computationCorrect = false;
            }

            if (daysWorked[employeeIndex] > 0) {
                double maxPossibleHours = daysWorked[employeeIndex] * 8.0;

                // Total worked hours should not exceed 8 hours per day.
                if (totalHoursWorked[employeeIndex] > maxPossibleHours) {
                    computationCorrect = false;
                }
            }

            double expectedSalary = totalHoursWorked[employeeIndex] * hourlyRates[employeeIndex];
            if (Math.abs(semiMonthlySalary[employeeIndex] - expectedSalary) > 0.0001) {
                computationCorrect = false;
            }
        }

        System.out.println();

        if (hasRecord && computationCorrect) {
            System.out.println("Test passed: Computation is correct");
        } else {
            System.out.println("Test failed: Computation is incorrect");
        }

        pressEnterToReturn(input);
    }

    /** Prompts the user to choose a month. */
    public static int promptMonth(Scanner input) {
        System.out.println("Choose month number only:");
        System.out.println("0 = Back to Main Menu");
        System.out.println("6 = June");
        System.out.println("7 = July");
        System.out.println("8 = August");
        System.out.println("9 = September");
        System.out.println("10 = October");
        System.out.println("11 = November");
        System.out.println("12 = December");

        return readValidIntWithBack(input, "Enter month: ", "Invalid month. Please enter 0 or a month from 6 to 12.", 6, 12);
    }

    /** Prompts the user to choose a cutoff. */
    public static int promptCutoff(Scanner input) {
        System.out.println();
        System.out.println("Choose cutoff:");
        System.out.println("0 = Back to Main Menu");
        System.out.println("1 = First Cutoff (1-15)");
        System.out.println("2 = Second Cutoff (16-end of month)");

        return readValidIntWithBack(input, "Enter cutoff: ", "Invalid cutoff. Please enter 0, 1, or 2.", 1, 2);
    }

    /** Prompts the user to choose the report view. */
    public static int promptViewOption(Scanner input) {
        System.out.println();
        System.out.println("Choose report view:");
        System.out.println("0 = Back to Main Menu");
        System.out.println("1 = View all employees");
        System.out.println("2 = View one employee only");

        return readValidIntWithBack(input, "Enter option: ", "Invalid report view option. Please enter 0, 1, or 2.", 1, 2);
    }

    /** Prompts the user to enter an employee number. */
    public static int promptEmployeeNumber(Scanner input) {
        System.out.println("Enter 0 to go back to the main menu.");
        return readPositiveIntWithBack(input, "Enter employee number: ", "Invalid employee number. Please enter 0 or a positive whole number.");
    }

    /** Pauses the program until the user presses Enter. */
    public static void pressEnterToReturn(Scanner input) {
        System.out.println();
        System.out.print("Press Enter to return to the main menu...");
        input.nextLine();
    }

    /** Reads a valid whole number with 0 as a back option. */
    public static int readValidIntWithBack(Scanner input, String prompt, String errorMessage, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String userInputLine = input.nextLine().trim();

            try {
                int enteredValue = Integer.parseInt(userInputLine);

                if (enteredValue == 0) {
                    System.out.println("Returning to main menu...");
                    return 0;
                }

                if (enteredValue >= min && enteredValue <= max) {
                    return enteredValue;
                } else {
                    System.out.println(errorMessage);
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a whole number only.");
            }
        }
    }

    /** Reads a positive integer with 0 as a back option. */
    public static int readPositiveIntWithBack(Scanner input, String prompt, String errorMessage) {
        while (true) {
            System.out.print(prompt);
            String userInputLine = input.nextLine().trim();

            try {
                int enteredValue = Integer.parseInt(userInputLine);

                if (enteredValue == 0) {
                    System.out.println("Returning to main menu...");
                    return 0;
                }

                if (enteredValue > 0) {
                    return enteredValue;
                } else {
                    System.out.println(errorMessage);
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a whole number only.");
            }
        }
    }

    /** Finds the array index of the given employee number. */
    public static int findEmployeeIndex(int[] employeeNumbers, int employeeCount, int employeeNumber) {
        for (int employeeIndex = 0; employeeIndex < employeeCount; employeeIndex++) {
            if (employeeNumbers[employeeIndex] == employeeNumber) {
                return employeeIndex;
            }
        }
        return -1;
    }

    /** Converts HH:MM time format to total minutes. */
    public static int convertTimeToMinutes(String time) {
        String[] timeParts = time.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);
        return (hour * 60) + minute;
    }

    /** Returns the month name based on the month number. */
    public static String getMonthName(int month) {
        if (month == 6) return "June";
        if (month == 7) return "July";
        if (month == 8) return "August";
        if (month == 9) return "September";
        if (month == 10) return "October";
        if (month == 11) return "November";
        if (month == 12) return "December";
        return "Invalid Month";
    }

    /** Returns the cutoff label. */
    public static String getCutoffName(int cutoff) {
        if (cutoff == 1) {
            return "First Cutoff";
        } else {
            return "Second Cutoff";
        }
    }

    /** Returns the day coverage of the selected cutoff. */
    public static String getCutoffCoverage(int cutoff) {
        if (cutoff == 1) {
            return "Days 1 to 15";
        } else {
            return "Days 16 to end of month";
        }
    }
}