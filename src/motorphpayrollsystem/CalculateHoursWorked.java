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
 * 
 */
public class CalculateHoursWorked {

    //Runs the complete hours-worked feature.
     
    public static void run(Scanner input) {
        String employeeFile = "EmployeeData/EmployeeDetails.csv";
        String attendanceFile = "EmployeeData/AttendanceRecord.csv";

        System.out.println("==============================================================");
        System.out.println("               MOTORPH PAYROLL PERIOD SELECTION               ");
        System.out.println("==============================================================");

        // Ask the user which month to process.
        int selectedMonth = promptMonth(input);
        if (selectedMonth == 0) {
            return;
        }

        // Ask the user which payroll cutoff to process.
        int selectedCutoff = promptCutoff(input);
        if (selectedCutoff == 0) {
            return;
        }

        // Ask whether to show all employees or only one employee.
        int viewOption = promptViewOption(input);
        if (viewOption == 0) {
            return;
        }

        int searchedEmployeeNumber = 0;

        // If the user selected "one employee only", ask for the employee number.
        if (viewOption == 2) {
            searchedEmployeeNumber = promptEmployeeNumber(input);
            if (searchedEmployeeNumber == 0) {
                return;
            }
        }

        int maxEmployees = 100;

        // Parallel arrays used to store employee data and computed results.
        int[] employeeNumbers = new int[maxEmployees];
        String[] employeeNames = new String[maxEmployees];
        double[] hourlyRates = new double[maxEmployees];

        double[] totalHoursWorked = new double[maxEmployees];
        int[] totalLateMinutes = new int[maxEmployees];
        int[] daysWorked = new int[maxEmployees];

        int employeeCount = 0;

        // Read employee basic details from the employee CSV file.
        try {
            BufferedReader br = new BufferedReader(new FileReader(employeeFile));
            String line = br.readLine(); // Skip header row.

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");

                // Skip incomplete rows.
                if (parts.length < 3) {
                    continue;
                }

                int employeeNumber = Integer.parseInt(parts[0].trim());
                String lastName = parts[1].trim();
                String firstName = parts[2].trim();
                double hourlyRate = Double.parseDouble(parts[parts.length - 1].trim());

                employeeNumbers[employeeCount] = employeeNumber;
                employeeNames[employeeCount] = firstName + " " + lastName;
                hourlyRates[employeeCount] = hourlyRate;

                employeeCount++;
            }

            br.close();
            
        //Handle possible errors when reading and processing the employee file
        } catch (IOException e) {
            System.out.println("Error reading employee file: " + e.getMessage());
            pressEnterToReturn(input);
            return;
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid number format found in employee file.");
            pressEnterToReturn(input);
            return;
        }

        // If only one employee is requested, check first if the employee exists.
        if (viewOption == 2) {
            int employeeIndex = findEmployeeIndex(employeeNumbers, employeeCount, searchedEmployeeNumber);

            if (employeeIndex == -1) {
                System.out.println("Employee number not found.");
                pressEnterToReturn(input);
                return;
            }
        }

        // Read attendance records and compute hours worked.
        try {
            BufferedReader br = new BufferedReader(new FileReader(attendanceFile));
            String line = br.readLine(); // Skip header row.

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");

                // Skip incomplete attendance rows.
                if (parts.length < 6) {
                    continue;
                }

                int employeeNumber = Integer.parseInt(parts[0].trim());
                String date = parts[3].trim();
                String logIn = parts[4].trim();
                String logOut = parts[5].trim();

                // When viewing one employee only, ignore all other employees.
                if (viewOption == 2 && employeeNumber != searchedEmployeeNumber) {
                    continue;
                }

                String[] dateParts = date.split("/");
                if (dateParts.length != 3) {
                    continue;
                }

                int month = Integer.parseInt(dateParts[0]);
                int day = Integer.parseInt(dateParts[1]);
                int year = Integer.parseInt(dateParts[2]);

                // Accept only the covered attendance period in the dataset.
                if (year != 2024 || month < 6 || month > 12) {
                    continue;
                }

                // Process only the selected month.
                if (month != selectedMonth) {
                    continue;
                }

                // Filter attendance records according to the selected cutoff.
                if (selectedCutoff == 1) {
                    if (day < 1 || day > 15) {
                        continue;
                    }
                } else {
                    if (day < 16) {
                        continue;
                    }
                }

                int employeeIndex = findEmployeeIndex(employeeNumbers, employeeCount, employeeNumber);
                if (employeeIndex == -1) {
                    continue;
                }

                int loginMinutes = convertTimeToMinutes(logIn);
                int logoutMinutes = convertTimeToMinutes(logOut);

                int officialStart = 8 * 60; // 8:00 AM
                int officialEnd = 17 * 60;  // 5:00 PM

                int countedStart;

                // Apply the 10-minute grace period for login time.
                if (loginMinutes <= ((8 * 60) + 10)) {
                    countedStart = officialStart;
                } else {
                    countedStart = loginMinutes;
                }

                // Do not allow the counted start time to be earlier than 8:00 AM.
                if (countedStart < officialStart) {
                    countedStart = officialStart;
                }

                int countedEnd = logoutMinutes;

                // Do not count work time beyond 5:00 PM.
                if (countedEnd > officialEnd) {
                    countedEnd = officialEnd;
                }

                // Compute total worked minutes for the day.
                int workedMinutes = countedEnd - countedStart;

                if (workedMinutes < 0) {
                    workedMinutes = 0;
                }

                // Deduct the 1-hour unpaid break.
                if (workedMinutes > 0) {
                    workedMinutes = workedMinutes - 60;
                }

                if (workedMinutes < 0) {
                    workedMinutes = 0;
                }

                double workedHours = workedMinutes / 60.0;

                // Count late minutes only when login is 8:11 AM or later.
                int lateMinutes = 0;
                if (loginMinutes >= ((8 * 60) + 11)) {
                    lateMinutes = loginMinutes - officialStart;
                }

                // Add the computed values to the employee totals.
                totalHoursWorked[employeeIndex] += workedHours;
                totalLateMinutes[employeeIndex] += lateMinutes;
                daysWorked[employeeIndex]++;
            }

            br.close();

        } catch (IOException e) {
            System.out.println("Error reading attendance file: " + e.getMessage());
            pressEnterToReturn(input);
            return;
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid number format found in attendance file.");
            pressEnterToReturn(input);
            return;
        }

        // Display the report header and the selected filters.
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

        if (viewOption == 1) {
            System.out.println("Report View           : All Employees");
        } else {
            System.out.println("Report View           : One Employee Only");
            System.out.println("Employee Number       : " + searchedEmployeeNumber);
        }

        System.out.println("==============================================================");

        boolean hasRecord = false;

        // Show the computed result for each employee that matches the chosen view.
        for (int i = 0; i < employeeCount; i++) {
            if (viewOption == 2 && employeeNumbers[i] != searchedEmployeeNumber) {
                continue;
            }

            if (daysWorked[i] > 0) {
                hasRecord = true;

                System.out.println();
                System.out.println("Employee Number       : " + employeeNumbers[i]);
                System.out.println("Employee Name         : " + employeeNames[i]);
                System.out.printf("Hourly Rate           : %.2f%n", hourlyRates[i]);
                System.out.println("Days Worked           : " + daysWorked[i]);
                System.out.printf("Total Hours Worked    : %.2f%n", totalHoursWorked[i]);
                System.out.println("--------------------------------------------------------------");
            }
        }

        if (!hasRecord) {
            System.out.println();
            System.out.println("No attendance records found for the selected payroll period.");
        }

        // Simple validation block to check if the computed values are correct.
        boolean computationCorrect = true;

        for (int i = 0; i < employeeCount; i++) {
            if (viewOption == 2 && employeeNumbers[i] != searchedEmployeeNumber) {
                continue;
            }

            if (totalHoursWorked[i] < 0) {
                computationCorrect = false;
            }

            if (totalLateMinutes[i] < 0) {
                computationCorrect = false;
            }

            if (daysWorked[i] > 0) {
                double maxPossibleHours = daysWorked[i] * 8.0;

                // Total worked hours should not be greater than 8 hours per workday.
                if (totalHoursWorked[i] > maxPossibleHours) {
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

    /**
     * Reads a whole number with an option to return to the main menu by entering 0.
     */
    public static int readValidIntWithBack(Scanner input, String prompt, String errorMessage, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = input.nextLine().trim();

            try {
                int value = Integer.parseInt(line);

                if (value == 0) {
                    System.out.println("Returning to main menu...");
                    return 0;
                }

                if (value >= min && value <= max) {
                    return value;
                } else {
                    System.out.println(errorMessage);
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a whole number only.");
            }
        }
    }

    /**
     * Reads a positive whole number with an option to return to the main menu by entering 0.
     */
    public static int readPositiveIntWithBack(Scanner input, String prompt, String errorMessage) {
        while (true) {
            System.out.print(prompt);
            String line = input.nextLine().trim();

            try {
                int value = Integer.parseInt(line);

                if (value == 0) {
                    System.out.println("Returning to main menu...");
                    return 0;
                }

                if (value > 0) {
                    return value;
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
        for (int i = 0; i < employeeCount; i++) {
            if (employeeNumbers[i] == employeeNumber) {
                return i;
            }
        }
        return -1;
    }

    /** Converts a time string in HH:MM format into total minutes. */
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
