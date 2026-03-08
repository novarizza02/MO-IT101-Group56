package motorphpayrollsystem;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Scanner;

/**
 * ComputeNetPay.java
 *
 * This class computes the monthly gross pay, government deductions,
 * and resulting net pay of employees using employee and attendance data.
 *
 * The report can be viewed for all employees or for one employee only.
 *
 * Rules used in this program:
 * - Cutoff 1 covers days 1 to 15.
 * - Cutoff 2 covers days 16 to end of month.
 * - A 10-minute grace period is applied for login.
 * - A 1-hour break is deducted for each valid workday.
 * - Monthly deductions are charged against the second cutoff net pay.
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
public class ComputeNetPay {

    /** Displays the report header. */
    public static void displayHeader() {
        System.out.println("==============================================================");
        System.out.println("                 MOTORPH SALARY CALCULATION                   ");
        System.out.println("==============================================================");
    }

    //Runs the net pay computation feature.
     
    public static void run(Scanner input) {
        displayHeader();

        String employeeFile = "EmployeeData/EmployeeDetails.csv";
        String attendanceFile = "EmployeeData/AttendanceRecord.csv";

        // Ask the user which month to compute.
        int selectedMonth = promptMonth(input);
        if (selectedMonth == 0) {
            return;
        }

        // Ask if the report should show all employees or only one employee.
        int viewOption = promptViewOption(input);
        if (viewOption == 0) {
            return;
        }

        int searchedEmployeeNumber = 0;

        // If only one employee is selected, ask for the employee number.
        if (viewOption == 2) {
            searchedEmployeeNumber = promptEmployeeNumber(input);
            if (searchedEmployeeNumber == 0) {
                return;
            }
        }

        int maxEmployees = 100;

        // Parallel arrays for employee details and computed payroll values.
        int[] employeeNumbers = new int[maxEmployees];
        String[] employeeNames = new String[maxEmployees];
        String[] employeeBirthdays = new String[maxEmployees];
        double[] hourlyRates = new double[maxEmployees];

        double[] cutoff1Hours = new double[maxEmployees];
        double[] cutoff2Hours = new double[maxEmployees];

        int[] cutoff1Days = new int[maxEmployees];
        int[] cutoff2Days = new int[maxEmployees];

        double[] cutoff1GrossPay = new double[maxEmployees];
        double[] cutoff2GrossPay = new double[maxEmployees];
        double[] monthlyGrossPay = new double[maxEmployees];

        double[] sssDeduction = new double[maxEmployees];
        double[] philHealthDeduction = new double[maxEmployees];
        double[] pagIbigDeduction = new double[maxEmployees];
        double[] taxDeduction = new double[maxEmployees];
        double[] totalDeductions = new double[maxEmployees];

        double[] cutoff1NetPay = new double[maxEmployees];
        double[] cutoff2NetPay = new double[maxEmployees];

        int employeeCount = 0;

        // Read employee details from the CSV file.
        try {
            BufferedReader br = new BufferedReader(new FileReader(employeeFile));
            String line = br.readLine(); // Skip header row.

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");

                // Skip incomplete rows.
                if (parts.length < 19) {
                    continue;
                }

                int empNumber = Integer.parseInt(parts[0].trim());
                String lastName = parts[1].trim();
                String firstName = parts[2].trim();
                String birthday = parts[3].trim();
                double hourlyRate = Double.parseDouble(parts[18].trim());

                employeeNumbers[employeeCount] = empNumber;
                employeeNames[employeeCount] = firstName + " " + lastName;
                employeeBirthdays[employeeCount] = birthday;
                hourlyRates[employeeCount] = hourlyRate;

                employeeCount++;
            }

            br.close();

        } catch (Exception e) {
            System.out.println("Error reading employee file: " + e.getMessage());
            pressEnterToReturn(input);
            return;
        }

        // If one employee is selected, verify first that the employee exists.
        if (viewOption == 2) {
            int employeeIndex = findEmployeeIndex(employeeNumbers, employeeCount, searchedEmployeeNumber);

            if (employeeIndex == -1) {
                System.out.println("Employee number not found.");
                pressEnterToReturn(input);
                return;
            }
        }

        // Read attendance records and compute hours worked per cutoff.
        try {
            BufferedReader br = new BufferedReader(new FileReader(attendanceFile));
            String line = br.readLine(); // Skip header row.

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");

                // Skip incomplete attendance rows.
                if (parts.length < 6) {
                    continue;
                }

                int empNumber = Integer.parseInt(parts[0].trim());
                String date = parts[3].trim();
                String logIn = parts[4].trim();
                String logOut = parts[5].trim();

                // When viewing one employee only, ignore other records.
                if (viewOption == 2 && empNumber != searchedEmployeeNumber) {
                    continue;
                }

                String[] dateParts = date.split("/");
                if (dateParts.length != 3) {
                    continue;
                }

                int month = Integer.parseInt(dateParts[0]);
                int day = Integer.parseInt(dateParts[1]);

                // Process only records from the selected month.
                if (month != selectedMonth) {
                    continue;
                }

                int empIndex = findEmployeeIndex(employeeNumbers, employeeCount, empNumber);
                if (empIndex == -1) {
                    continue;
                }

                int loginMinutes = convertTimeToMinutes(logIn);
                int logoutMinutes = convertTimeToMinutes(logOut);

                int officialStart = 480; // 8:00 AM
                int officialEnd = 1020;  // 5:00 PM

                int countedStart;

                // Apply the grace period: 8:00 AM to 8:10 AM counts as 8:00 AM.
                if (loginMinutes <= 490) {
                    countedStart = officialStart;
                } else {
                    countedStart = loginMinutes;
                }

                if (countedStart < officialStart) {
                    countedStart = officialStart;
                }

                int countedEnd = logoutMinutes;

                // Do not count work rendered after 5:00 PM.
                if (countedEnd > officialEnd) {
                    countedEnd = officialEnd;
                }

                int workedMinutes = countedEnd - countedStart;

                // Deduct one hour for break if there is valid worked time.
                if (workedMinutes > 0) {
                    workedMinutes -= 60;
                }

                if (workedMinutes < 0) {
                    workedMinutes = 0;
                }

                double workedHours = workedMinutes / 60.0;

                // Add worked hours to the proper payroll cutoff.
                if (day <= 15) {
                    cutoff1Hours[empIndex] += workedHours;
                    cutoff1Days[empIndex]++;
                } else {
                    cutoff2Hours[empIndex] += workedHours;
                    cutoff2Days[empIndex]++;
                }
            }

            br.close();

        } catch (Exception e) {
            System.out.println("Error reading attendance file: " + e.getMessage());
            pressEnterToReturn(input);
            return;
        }

        // Compute gross pay, deductions, and net pay for every employee loaded in memory.
        for (int i = 0; i < employeeCount; i++) {
            cutoff1GrossPay[i] = cutoff1Hours[i] * hourlyRates[i];
            cutoff2GrossPay[i] = cutoff2Hours[i] * hourlyRates[i];

            monthlyGrossPay[i] = cutoff1GrossPay[i] + cutoff2GrossPay[i];

            sssDeduction[i] = computeSSS(monthlyGrossPay[i]);
            philHealthDeduction[i] = computePhilHealth(monthlyGrossPay[i]);
            pagIbigDeduction[i] = computePagIbig(monthlyGrossPay[i]);
            taxDeduction[i] = computeIncomeTax(monthlyGrossPay[i]);
            totalDeductions[i] = computeTotalDeductions(monthlyGrossPay[i]);

            // In this program, first cutoff net pay is shown without deductions.
            cutoff1NetPay[i] = cutoff1GrossPay[i];

            // Monthly deductions are subtracted from second cutoff pay.
            cutoff2NetPay[i] = cutoff2GrossPay[i] - totalDeductions[i];

            // Prevent negative net pay values from being shown.
            if (cutoff2NetPay[i] < 0) {
                cutoff2NetPay[i] = 0;
            }
        }

        System.out.println();
        System.out.println("==============================================================");
        System.out.println("                   MOTORPH SALARY COMPUTATION                 ");
        System.out.println("==============================================================");

        boolean hasRecord = false;

        // Display the payroll report for each matching employee.
        for (int i = 0; i < employeeCount; i++) {
            if (viewOption == 2 && employeeNumbers[i] != searchedEmployeeNumber) {
                continue;
            }

            if (cutoff1Days[i] > 0 || cutoff2Days[i] > 0) {
                hasRecord = true;

                System.out.println("==============================================================");
                System.out.println("Employee Number       : " + employeeNumbers[i]);
                System.out.println("Employee Name         : " + employeeNames[i]);
                System.out.println("Birthday              : " + employeeBirthdays[i]);
                System.out.println("==============================================================");

                System.out.println("Selected Month        : " + getMonthName(selectedMonth));
                System.out.println("1ST CUTOFF (" + getFirstCutoffCoverage(selectedMonth) + ")");
                System.out.println("Days Worked           : " + cutoff1Days[i]);
                System.out.printf("Hours Worked          : %.2f%n", cutoff1Hours[i]);
                System.out.printf("Gross Pay             : %.2f%n", cutoff1GrossPay[i]);
                System.out.printf("Net Pay               : %.2f%n", cutoff1NetPay[i]);
                System.out.println("--------------------------------------------");

                System.out.println("2ND CUTOFF (" + getSecondCutoffCoverage(selectedMonth) + ")");
                System.out.println("Days Worked           : " + cutoff2Days[i]);
                System.out.printf("Hours Worked          : %.2f%n", cutoff2Hours[i]);
                System.out.printf("Gross Pay             : %.2f%n", cutoff2GrossPay[i]);
                System.out.println("--------------------------------------------");

                System.out.println("MONTHLY SUMMARY");
                System.out.printf("Monthly Gross Pay     : %.2f%n", monthlyGrossPay[i]);
                System.out.printf("SSS Deduction         : %.2f%n", sssDeduction[i]);
                System.out.printf("PhilHealth Deduction  : %.2f%n", philHealthDeduction[i]);
                System.out.printf("Pag-IBIG Deduction    : %.2f%n", pagIbigDeduction[i]);
                System.out.printf("Withholding Tax       : %.2f%n", taxDeduction[i]);
                System.out.printf("Total Deductions      : %.2f%n", totalDeductions[i]);
                System.out.printf("2nd Cutoff Net Pay    : %.2f%n", cutoff2NetPay[i]);
                System.out.println("--------------------------------------------");
                System.out.println();
                System.out.println();
                System.out.println();
                System.out.println("==============================================================");
            }
        }

        if (!hasRecord) {
            System.out.println();
            System.out.println("No attendance records found for the selected month.");
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

    /** Prompts the user to choose whether to view all employees or one employee. */
    public static int promptViewOption(Scanner input) {
        System.out.println();
        System.out.println("Choose report view:");
        System.out.println("0 = Back to Main Menu");
        System.out.println("1 = View all employees");
        System.out.println("2 = View one employee only");

        return readValidIntWithBack(input, "Enter option: ", "Invalid option. Please enter 0, 1, or 2.", 1, 2);
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

    /** Reads a whole-number input with 0 as a back option. */
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

    /** Reads a positive whole number with 0 as a back option. */
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

    /** Finds the array index of the employee number. */
    public static int findEmployeeIndex(int[] employeeNumbers, int employeeCount, int employeeNumber) {
        for (int i = 0; i < employeeCount; i++) {
            if (employeeNumbers[i] == employeeNumber) {
                return i;
            }
        }
        return -1;
    }

    /** Converts HH:MM time format into total minutes. */
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

    /** Returns the first cutoff coverage label. */
    public static String getFirstCutoffCoverage(int month) {
        return getMonthName(month) + " 1-15";
    }

    /** Returns the second cutoff coverage label. */
    public static String getSecondCutoffCoverage(int month) {
        return getMonthName(month) + " 16-end";
    }

    /**
     * Reads the SSS contribution table from sss.csv and returns the matching contribution.
     */
    public static double computeSSS(double monthlyGrossPay) {
        String filePath = "EmployeeData/sss.csv";

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine(); // Skip header row.

            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");

                if (data.length < 3) {
                    continue;
                }

                double salaryFrom = Double.parseDouble(data[0]);
                double salaryTo = Double.parseDouble(data[1]);
                double contribution = Double.parseDouble(data[2]);

                if (monthlyGrossPay >= salaryFrom && monthlyGrossPay <= salaryTo) {
                    return contribution;
                }
            }

        } catch (Exception e) {
            System.out.println("Error reading sss.csv: " + e.getMessage());
        }

        return 0.0;
    }

    /** Computes the employee share of PhilHealth contribution. */
    public static double computePhilHealth(double monthlyGrossPay) {
        double salaryBasis;

        if (monthlyGrossPay <= 10000) {
            salaryBasis = 10000;
        } else if (monthlyGrossPay >= 60000) {
            salaryBasis = 60000;
        } else {
            salaryBasis = monthlyGrossPay;
        }

        double premium = salaryBasis * 0.03;
        return premium / 2;
    }

    /** Computes the Pag-IBIG contribution with a maximum employee share of 100. */
    public static double computePagIbig(double monthlyGrossPay) {
        double contribution;

        if (monthlyGrossPay <= 1500) {
            contribution = monthlyGrossPay * 0.01;
        } else {
            contribution = monthlyGrossPay * 0.02;
        }

        if (contribution > 100) {
            contribution = 100;
        }

        return contribution;
    }

    /** Computes withholding tax based on taxable income after mandatory deductions. */
    public static double computeIncomeTax(double monthlyGrossPay) {
        double sss = computeSSS(monthlyGrossPay);
        double philHealth = computePhilHealth(monthlyGrossPay);
        double pagIbig = computePagIbig(monthlyGrossPay);

        double taxableIncome = monthlyGrossPay - sss - philHealth - pagIbig;

        if (taxableIncome <= 20832) return 0;
        if (taxableIncome <= 33332) return (taxableIncome - 20833) * 0.20;
        if (taxableIncome <= 66666) return 2500 + ((taxableIncome - 33333) * 0.25);
        if (taxableIncome <= 166666) return 10833 + ((taxableIncome - 66667) * 0.30);
        if (taxableIncome <= 666666) return 40833.33 + ((taxableIncome - 166667) * 0.32);

        return 200833.33 + ((taxableIncome - 666667) * 0.35);
    }

    /** Computes the total monthly deductions. */
    public static double computeTotalDeductions(double monthlyGrossPay) {
        double sss = computeSSS(monthlyGrossPay);
        double philHealth = computePhilHealth(monthlyGrossPay);
        double pagIbig = computePagIbig(monthlyGrossPay);
        double tax = computeIncomeTax(monthlyGrossPay);

        return sss + philHealth + pagIbig + tax;
    }
}
