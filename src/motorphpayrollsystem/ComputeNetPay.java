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
 */
public class ComputeNetPay {

    /** Displays the report header. */
    public static void displayHeader() {
        System.out.println("==============================================================");
        System.out.println("                 MOTORPH SALARY CALCULATION                   ");
        System.out.println("==============================================================");
    }

    /** Runs the net pay computation feature. */
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

        // Read employee details from the employee CSV file.
        try {
            BufferedReader br = new BufferedReader(new FileReader(employeeFile));
            String line = br.readLine(); // Skip header row.

            while ((line = br.readLine()) != null) {

                // Remove quotation marks before splitting the CSV row.
                line = line.replace("\"", "");
                String[] parts = line.split(",");

                // Skip incomplete rows.
                if (parts.length < 4) {
                    continue;
                }

                int empNumber = Integer.parseInt(parts[0].trim());
                String lastName = parts[1].trim();
                String firstName = parts[2].trim();
                String birthday = parts[3].trim();

                // The last field in the employee CSV is treated as the hourly rate.
                double hourlyRate = Double.parseDouble(parts[parts.length - 1].trim());

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
                int year = Integer.parseInt(dateParts[2]);

                // Accept only attendance records within the project dataset coverage.
                if (year != 2024 || month < 6 || month > 12) {
                    continue;
                }

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

                int officialStart = 8 * 60; // 8:00 AM
                int officialEnd = 17 * 60;  // 5:00 PM

                int countedStart;

                // Apply the 10-minute grace period.
                // 8:00 AM to 8:10 AM is still counted as 8:00 AM.
                if (loginMinutes <= ((8 * 60) + 10)) {
                    countedStart = officialStart;
                } else {
                    countedStart = loginMinutes;
                }

                // Do not count work before the official start of shift.
                if (countedStart < officialStart) {
                    countedStart = officialStart;
                }

                int countedEnd = logoutMinutes;

                // Do not count work after the official end of shift.
                if (countedEnd > officialEnd) {
                    countedEnd = officialEnd;
                }

                int workedMinutes = countedEnd - countedStart;

                // Deduct one hour for break if there is valid worked time.
                if (workedMinutes > 0) {
                    workedMinutes -= 60;
                }

                // Prevent negative worked time.
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
                System.out.println("Hours Worked          : " + cutoff1Hours[i]);
                System.out.println("Gross Pay             : " + cutoff1GrossPay[i]);
                System.out.println("Net Pay               : " + cutoff1NetPay[i]);
                System.out.println("--------------------------------------------");

                System.out.println("2ND CUTOFF (" + getSecondCutoffCoverage(selectedMonth) + ")");
                System.out.println("Days Worked           : " + cutoff2Days[i]);
                System.out.println("Hours Worked          : " + cutoff2Hours[i]);
                System.out.println("Gross Pay             : " + cutoff2GrossPay[i]);
                System.out.println("--------------------------------------------");

                System.out.println("MONTHLY SUMMARY");
                System.out.println("Monthly Gross Pay     : " + monthlyGrossPay[i]);
                System.out.println("SSS Deduction         : " + sssDeduction[i]);
                System.out.println("PhilHealth Deduction  : " + philHealthDeduction[i]);
                System.out.println("Pag-IBIG Deduction    : " + pagIbigDeduction[i]);
                System.out.println("Withholding Tax       : " + taxDeduction[i]);
                System.out.println("Total Deductions      : " + totalDeductions[i]);
                System.out.println("2nd Cutoff Net Pay    : " + cutoff2NetPay[i]);
                System.out.println("--------------------------------------------");
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

    //Computes SSS contribution using arrays
     
    public static double computeSSS(double monthlyGrossPay) {
        double[] salaryFrom = {
            0, 3250, 3750, 4250, 4750, 5250, 5750, 6250, 6750, 7250,
            7750, 8250, 8750, 9250, 9750, 10250, 10750, 11250, 11750, 12250,
            12750, 13250, 13750, 14250, 14750, 15250, 15750, 16250, 16750, 17250,
            17750, 18250, 18750, 19250, 19750, 20250, 20750, 21250, 21750, 22250,
            22750, 23250, 23750, 24250, 24750
        };

        double[] salaryTo = {
            3249.99, 3749.99, 4249.99, 4749.99, 5249.99, 5749.99, 6249.99, 6749.99, 7249.99, 7749.99,
            8249.99, 8749.99, 9249.99, 9749.99, 10249.99, 10749.99, 11249.99, 11749.99, 12249.99, 12749.99,
            13249.99, 13749.99, 14249.99, 14749.99, 15249.99, 15749.99, 16249.99, 16749.99, 17249.99, 17749.99,
            18249.99, 18749.99, 19249.99, 19749.99, 20249.99, 20749.99, 21249.99, 21749.99, 22249.99, 22749.99,
            23249.99, 23749.99, 24249.99, 24749.99, 999999.99
        };

        double[] contribution = {
            135.00, 157.50, 180.00, 202.50, 225.00, 247.50, 270.00, 292.50, 315.00, 337.50,
            360.00, 382.50, 405.00, 427.50, 450.00, 472.50, 495.00, 517.50, 540.00, 562.50,
            585.00, 607.50, 630.00, 652.50, 675.00, 697.50, 720.00, 742.50, 765.00, 787.50,
            810.00, 832.50, 855.00, 877.50, 900.00, 922.50, 945.00, 967.50, 990.00, 1012.50,
            1035.00, 1057.50, 1080.00, 1102.50, 1125.00
        };

        // Loop through each SSS bracket and return the matching contribution.
        for (int i = 0; i < salaryFrom.length; i++) {
            if (monthlyGrossPay >= salaryFrom[i] && monthlyGrossPay <= salaryTo[i]) {
                return contribution[i];
            }
        }

        return 0.0;
    }

    /** Computes the employee share of PhilHealth contribution. */
    public static double computePhilHealth(double monthlyGrossPay) {

        double premium;

        if (monthlyGrossPay >= 60000) {
        // Apply maximum cap
        premium = 60000 * 0.03;
        } else {
        // Use actual gross pay
        premium = monthlyGrossPay * 0.03;
        }

        // Return employee share (50%)
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

    /** Computes total deductions by adding all mandatory contributions and tax. */
    public static double computeTotalDeductions(double monthlyGrossPay) {
        return computeSSS(monthlyGrossPay)
                + computePhilHealth(monthlyGrossPay)
                + computePagIbig(monthlyGrossPay)
                + computeIncomeTax(monthlyGrossPay);
    }
}