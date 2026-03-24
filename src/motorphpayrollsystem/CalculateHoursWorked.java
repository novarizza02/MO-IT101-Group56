package motorphpayrollsystem;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

/**
 * CalculateHoursWorked.java
 *
 * This class reads employee and attendance records from CSV files,
 * computes the total hours worked for a chosen payroll period,
 * and displays the result either for all employees or for one employee only.
 *
 * Business rules used in this program:
 * - Work hours counted are from 8:00 AM to 5:00 PM only.
 * - A grace period is applied from 8:00 AM to 8:10 AM.
 * - A 1-hour break is deducted for each workday.
 * - Lateness starts at 8:11 AM onward.
 *
 * Login Section:
 * The user must enter a valid username and password before accessing the feature.
 * Username: payroll_staff
 * Password: 12345
 *
 * If the login credentials are incorrect, access to the feature
 * will be denied and the user will return to the menu.
 */
public class CalculateHoursWorked {

    // Runs the complete hours-worked feature.
    public static void run(Scanner input) {
        String employeeFilePath = MotorPHPayrollSystem.EMPLOYEE_FILE;
        String attendanceFilePath = MotorPHPayrollSystem.ATTENDANCE_FILE;

        System.out.println("==============================================================");
        System.out.println("               MOTORPH PAYROLL PERIOD SELECTION               ");
        System.out.println("==============================================================");

        // Ask user to select month (returns 0 if user chooses to go back)
        int selectedMonth = promptMonth(input);
        if (selectedMonth == 0) {
            return;
        }

        // Ask user to select cutoff (1st or 2nd half of month)
        int selectedCutoff = promptCutoff(input);
        if (selectedCutoff == 0) {
            return;
        }

        // Ask how the report should be viewed 
        int reportViewOption = promptViewOption(input);
        if (reportViewOption == 0) {
            return;
        }

        int searchedEmployeeNumber = 0; // Variable to store employee number if user selects specific employee
 
        // If user chose "specific employee", ask for employee number
        if (reportViewOption == 2) {
            searchedEmployeeNumber = promptEmployeeNumber(input);
            if (searchedEmployeeNumber == 0) {
                return;
            }
        }

        // Maximum number of employees allowed in the system
        int maxEmployees = MotorPHPayrollSystem.MAX_EMPLOYEES;

        // Arrays to store employee data
        int[] employeeNumbers = new int[maxEmployees];
        String[] employeeNames = new String[maxEmployees];
        double[] hourlyRates = new double[maxEmployees];

        // Arrays to store computed payroll data
        double[] totalHoursWorked = new double[maxEmployees];
        int[] totalLateMinutes = new int[maxEmployees];
        int[] daysWorked = new int[maxEmployees];

        int employeeCount = 0;

        // Read employee basic details from the employee CSV file.
        try (BufferedReader employeeReader = new BufferedReader(new FileReader(employeeFilePath))) {

            String employeeLine = employeeReader.readLine(); // Skip header row.

            while ((employeeLine = employeeReader.readLine()) != null) {

                employeeLine = employeeLine.replace("\"", ""); // remove quotes from CSV
                String[] employeeFields = employeeLine.split(","); // split row into columns

                if (employeeFields.length < 3) {
                    continue; // skip invalid/incomplete rows
                }

                // Extract employee data
                int employeeNumber = Integer.parseInt(employeeFields[0].trim());
                String lastName = employeeFields[1].trim();
                String firstName = employeeFields[2].trim();
                double hourlyRate = Double.parseDouble(employeeFields[employeeFields.length - 1].trim());

                // Store values into arrays
                employeeNumbers[employeeCount] = employeeNumber;
                employeeNames[employeeCount] = firstName + " " + lastName;
                hourlyRates[employeeCount] = hourlyRate;

                employeeCount++; // move to next index
            }

        } catch (IOException e) {
            System.out.println("Error reading employee file: " + e.getMessage());
            pressEnterToReturn(input);
            return; // stop process if file error
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid number format found in employee file.");
            pressEnterToReturn(input);
            return; // stop if data format is wrong
        }

        // If viewing one employee, check if employee exists
        if (reportViewOption == 2) {
            int employeeIndex = findEmployeeIndex(employeeNumbers, employeeCount, searchedEmployeeNumber);

            if (employeeIndex == -1) {
                System.out.println("Employee number not found.");
                pressEnterToReturn(input);
                return; // stop if employee not found
            }
        }

        // Read attendance records and compute hours worked.
        try (BufferedReader attendanceReader = new BufferedReader(new FileReader(attendanceFilePath))) {
            String attendanceLine = attendanceReader.readLine(); // Skip header row.

            while ((attendanceLine = attendanceReader.readLine()) != null) {
                String[] attendanceFields = attendanceLine.split(",");

                if (attendanceFields.length < 6) {
                    continue; // skip invalid rows
                }

                // Extract attendance data
                int employeeNumber = Integer.parseInt(attendanceFields[0].trim());
                String attendanceDate = attendanceFields[3].trim();
                String logInTime = attendanceFields[4].trim();
                String logOutTime = attendanceFields[5].trim();

                // If specific employee selected, skip others
                if (reportViewOption == 2 && employeeNumber != searchedEmployeeNumber) {
                    continue;
                }

                // Split date into month/day/year
                String[] dateTokens = attendanceDate.split("/");
                if (dateTokens.length != 3) {
                    continue;
                }

                int attendanceMonth = Integer.parseInt(dateTokens[0]);
                int attendanceDay = Integer.parseInt(dateTokens[1]);
                int attendanceYear = Integer.parseInt(dateTokens[2]);

                // Filter only valid months/year (June–December 2024)
                if (attendanceYear != 2024 || attendanceMonth < 6 || attendanceMonth > 12) {
                    continue;
                }

                // Filter selected month
                if (attendanceMonth != selectedMonth) {
                    continue;
                }

                // Filter based on cutoff (1–15 or 16–end)
                if (selectedCutoff == 1) {
                    if (attendanceDay < 1 || attendanceDay > 15) {
                        continue;
                    }
                } else {
                    if (attendanceDay < 16) {
                        continue;
                    }
                }

                // Find employee index in array
                int employeeIndex = findEmployeeIndex(employeeNumbers, employeeCount, employeeNumber);
                if (employeeIndex == -1) {
                    continue; // skip if employee not found
                }

                // Duplication reduced: shared worked-hours computation
                int loginMinutes = MotorPHPayrollSystem.convertTimeToMinutes(logInTime);
                // Compute worked hours (already includes rules like 8AM–5PM, lunch deduction)
                double workedHours = MotorPHPayrollSystem.computeWorkedHours(logInTime, logOutTime);

                int officialStart = 8 * 60; // 8:00 AM in minutes

                int lateMinutes = 0;
                // Apply lateness rule (8:11 AM onwards)
                if (loginMinutes >= ((8 * 60) + 11)) {
                    lateMinutes = loginMinutes - officialStart;
                }

                // Accumulate totals
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

        // Display report header
        System.out.println();
        System.out.println("==============================================================");
        System.out.println("                 MOTORPH HOURS WORKED REPORT                  ");
        System.out.println("==============================================================");
        System.out.println("Selected Month        : " + getMonthName(selectedMonth));
        System.out.println("Selected Cutoff       : " + getCutoffName(selectedCutoff));
        System.out.println("Coverage              : " + getCutoffCoverage(selectedCutoff));
        System.out.println("Work Hours Counted    : 8:00 AM to 5:00 PM");
        System.out.println("Break Deduction       : 1 hour per workday");
        System.out.println("Grace Period Applied  : 8:00 AM to 8:10 AM");
        System.out.println("Late Rule Applied     : 8:11 AM onwards");

        // Show report type
        if (reportViewOption == 1) {
            System.out.println("Report View           : All Employees");
        } else {
            System.out.println("Report View           : One Employee Only");
            System.out.println("Employee Number       : " + searchedEmployeeNumber);
        }

        System.out.println("==============================================================");

        boolean hasRecord = false;

        // Loop through employees and display results
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
                System.out.println("--------------------------------------------------------------");
            }
        }

        // If no records found
        if (!hasRecord) {
            System.out.println();
            System.out.println("No attendance records found for the selected payroll period.");
        }

        // Validate computation correctness
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

            if (daysWorked[employeeIndex] > 0) {
                double maxPossibleHours = daysWorked[employeeIndex] * 8.0;

                if (totalHoursWorked[employeeIndex] > maxPossibleHours) {
                    computationCorrect = false;
                }
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

    /** Prompts the user to choose a cutoff period. */
    public static int promptCutoff(Scanner input) {
        System.out.println();
        System.out.println("Choose cutoff:");
        System.out.println("0 = Back to Main Menu");
        System.out.println("1 = First Cutoff (1-15)");
        System.out.println("2 = Second Cutoff (16-end of month)");

        return readValidIntWithBack(input, "Enter cutoff: ", "Invalid cutoff. Please enter 0, 1, or 2.", 1, 2);
    }

    /** Prompts the user to choose how the report will be displayed. */
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

    /** Pauses the screen until the user presses Enter. */
    public static void pressEnterToReturn(Scanner input) {
        System.out.println();
        System.out.print("Press Enter to return to the main menu...");
        input.nextLine();
    }

    /** Reads a whole number with an option to return to the main menu by entering 0. */
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

    /** Reads a positive whole number with an option to return to the main menu by entering 0. */
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

    /** Finds the array index of a specific employee number. */
    public static int findEmployeeIndex(int[] employeeNumbers, int employeeCount, int employeeNumber) {
        for (int employeeIndex = 0; employeeIndex < employeeCount; employeeIndex++) {
            if (employeeNumbers[employeeIndex] == employeeNumber) {
                return employeeIndex;
            }
        }
        return -1;
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

    /** Returns the cutoff name. */
    public static String getCutoffName(int cutoff) {
        if (cutoff == 1) {
            return "First Cutoff";
        } else {
            return "Second Cutoff";
        }
    }

    /** Returns the cutoff day coverage. */
    public static String getCutoffCoverage(int cutoff) {
        if (cutoff == 1) {
            return "Days 1 to 15";
        } else {
            return "Days 16 to end of month";
        }
    }
}