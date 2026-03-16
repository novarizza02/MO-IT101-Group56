package motorphpayrollsystem;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Scanner;

/**
 * ViewEmployeeDetails.java
 *
 * This class allows an employee to log in using their employee number
 * and view their personal details, attendance-based payroll summary,
 * gross pay, deductions, and net pay for a selected month.
 *
 * Login Section:
 * Username: employee number (example: 10001)
 * Password: 12345
 *
 * If the login credentials are incorrect, access to the feature
 * will be denied and the user will return to the menu.
 * 
 * 
 * This module is intended for employee self-view only.
 */
public class ViewEmployeeDetails {

    /** Displays the employee portal header. */
    public static void displayHeader() {
        System.out.println("==============================================================");
        System.out.println("                  MOTORPH EMPLOYEE PORTAL                     ");
        System.out.println("==============================================================");
    }

    /** Runs the employee self-service details page. */
    public static void run(Scanner input) {
        displayHeader();

        String employeeFile = "EmployeeData/EmployeeDetails.csv";
        String attendanceFile = "EmployeeData/AttendanceRecord.csv";

        // Ask the employee to log in using employee number and password.
        System.out.println("Type 0 as username to go back to the main menu.");
        System.out.print("Enter username (Employee Number): ");
        String username = input.nextLine().trim();

        if (username.equals("0")) {
            System.out.println("Returning to main menu...");
            return;
        }

        System.out.print("Enter password: ");
        String password = input.nextLine().trim();

        int loggedInEmployeeNumber;

        // Convert the entered username into an integer employee number.
        try {
            loggedInEmployeeNumber = Integer.parseInt(username);
        } catch (NumberFormatException e) {
            System.out.println("Invalid username. Username must be a valid employee number.");
            pressEnterToReturn(input);
            return;
        }

        // Validate the employee password.
        if (!password.equals("12345")) {
            System.out.println("Invalid password.");
            pressEnterToReturn(input);
            return;
        }

        // Ask for the month to be viewed.
        int selectedMonth = promptMonth(input);
        if (selectedMonth == 0) {
            return;
        }

        int maxEmployees = 100;

        // Parallel arrays for employee information and computed payroll data.
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

        // Load employee details from the employee CSV file.
        try {
            BufferedReader br = new BufferedReader(new FileReader(employeeFile));
            String line = br.readLine(); // Skip header row.

            while ((line = br.readLine()) != null) {

                // Remove quotes from the CSV row before splitting.
                // This helps avoid issues when values are enclosed in quotes.
                line = line.replace("\"", "");
                String[] parts = line.split(",");

                // Skip incomplete rows.
                // We only need the first few fields and the last field.
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

        // Find the logged-in employee in the loaded employee data.
        int employeeIndex = findEmployeeIndex(employeeNumbers, employeeCount, loggedInEmployeeNumber);

        if (employeeIndex == -1) {
            System.out.println("Employee number not found.");
            pressEnterToReturn(input);
            return;
        }

        // Read attendance records and compute payroll values for the logged-in employee only.
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

                // Ignore attendance records that do not belong to the logged-in employee.
                if (empNumber != loggedInEmployeeNumber) {
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

                // Process only the month selected by the employee.
                if (month != selectedMonth) {
                    continue;
                }

                int loginMinutes = convertTimeToMinutes(logIn);
                int logoutMinutes = convertTimeToMinutes(logOut);

                int officialStart = 8 * 60; // 8:00 AM
                int officialEnd = 17 * 60;  // 5:00 PM

                int countedStart;

                // Apply the 10-minute grace period:
                // 8:00 AM to 8:10 AM is still counted as 8:00 AM.
                if (loginMinutes <= ((8 * 60) + 10)) {
                    countedStart = officialStart;
                } else {
                    countedStart = loginMinutes;
                }

                // Do not count work before the official shift start.
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

                // Prevent negative worked time values.
                if (workedMinutes < 0) {
                    workedMinutes = 0;
                }

                double workedHours = workedMinutes / 60.0;

                // Add the worked hours to cutoff 1 or cutoff 2 depending on the day.
                if (day <= 15) {
                    cutoff1Hours[employeeIndex] += workedHours;
                    cutoff1Days[employeeIndex]++;
                } else {
                    cutoff2Hours[employeeIndex] += workedHours;
                    cutoff2Days[employeeIndex]++;
                }
            }

            br.close();

        } catch (Exception e) {
            System.out.println("Error reading attendance file: " + e.getMessage());
            pressEnterToReturn(input);
            return;
        }

        // Compute payroll amounts for the logged-in employee.
        cutoff1GrossPay[employeeIndex] = cutoff1Hours[employeeIndex] * hourlyRates[employeeIndex];
        cutoff2GrossPay[employeeIndex] = cutoff2Hours[employeeIndex] * hourlyRates[employeeIndex];
        monthlyGrossPay[employeeIndex] = cutoff1GrossPay[employeeIndex] + cutoff2GrossPay[employeeIndex];

        sssDeduction[employeeIndex] = computeSSS(monthlyGrossPay[employeeIndex]);
        philHealthDeduction[employeeIndex] = computePhilHealth(monthlyGrossPay[employeeIndex]);
        pagIbigDeduction[employeeIndex] = computePagIbig(monthlyGrossPay[employeeIndex]);
        taxDeduction[employeeIndex] = computeIncomeTax(monthlyGrossPay[employeeIndex]);
        totalDeductions[employeeIndex] = computeTotalDeductions(monthlyGrossPay[employeeIndex]);

        // In this design, first cutoff pay is shown without deductions.
        cutoff1NetPay[employeeIndex] = cutoff1GrossPay[employeeIndex];

        // Monthly deductions are applied to the second cutoff pay.
        cutoff2NetPay[employeeIndex] = cutoff2GrossPay[employeeIndex] - totalDeductions[employeeIndex];

        if (cutoff2NetPay[employeeIndex] < 0) {
            cutoff2NetPay[employeeIndex] = 0;
        }

        // Display the employee's details and payroll summary.
        System.out.println();
        System.out.println("==============================================================");
        System.out.println("                  MOTORPH EMPLOYEE DETAILS                    ");
        System.out.println("==============================================================");
        System.out.println("Employee Number       : " + employeeNumbers[employeeIndex]);
        System.out.println("Employee Name         : " + employeeNames[employeeIndex]);
        System.out.println("Birthday              : " + employeeBirthdays[employeeIndex]);
        System.out.println("Selected Month        : " + getMonthName(selectedMonth));
        System.out.println("==============================================================");

        if (cutoff1Days[employeeIndex] > 0 || cutoff2Days[employeeIndex] > 0) {
            System.out.println();
            System.out.println("1ST CUTOFF (" + getFirstCutoffCoverage(selectedMonth) + ")");
            System.out.println("Days Worked           : " + cutoff1Days[employeeIndex]);
            System.out.printf("Hours Worked          : %.2f%n", cutoff1Hours[employeeIndex]);
            System.out.printf("Gross Pay             : %.2f%n", cutoff1GrossPay[employeeIndex]);
            System.out.printf("Net Pay               : %.2f%n", cutoff1NetPay[employeeIndex]);
            System.out.println("--------------------------------------------");

            System.out.println("2ND CUTOFF (" + getSecondCutoffCoverage(selectedMonth) + ")");
            System.out.println("Days Worked           : " + cutoff2Days[employeeIndex]);
            System.out.printf("Hours Worked          : %.2f%n", cutoff2Hours[employeeIndex]);
            System.out.printf("Gross Pay             : %.2f%n", cutoff2GrossPay[employeeIndex]);
            System.out.printf("Net Pay               : %.2f%n", cutoff2NetPay[employeeIndex]);
            System.out.println("--------------------------------------------");

            System.out.println("MONTHLY SUMMARY");
            System.out.printf("Monthly Gross Pay     : %.2f%n", monthlyGrossPay[employeeIndex]);
            System.out.printf("SSS Deduction         : %.2f%n", sssDeduction[employeeIndex]);
            System.out.printf("PhilHealth Deduction  : %.2f%n", philHealthDeduction[employeeIndex]);
            System.out.printf("Pag-IBIG Deduction    : %.2f%n", pagIbigDeduction[employeeIndex]);
            System.out.printf("Withholding Tax       : %.2f%n", taxDeduction[employeeIndex]);
            System.out.printf("Total Deductions      : %.2f%n", totalDeductions[employeeIndex]);
            System.out.println("--------------------------------------------");
            System.out.println();
            System.out.println("==============================================================");
        } else {
            System.out.println();
            System.out.println("No attendance records found for the selected month.");
        }

        pressEnterToReturn(input);
    }

    /** Prompts the employee to choose a month. */
    public static int promptMonth(Scanner input) {
        System.out.println();
        System.out.println("Choose month number only:");
        System.out.println("0 = Back to Main Menu");
        System.out.println("6 = June");
        System.out.println("7 = July");
        System.out.println("8 = August");
        System.out.println("9 = September");
        System.out.println("10 = October");
        System.out.println("11 = November");
        System.out.println("12 = December");

        return readValidIntWithBack(
                input,
                "Enter month: ",
                "Invalid month. Please enter 0 or a month from 6 to 12.",
                6,
                12
        );
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

    /** Finds the array index of the employee number. */
    public static int findEmployeeIndex(int[] employeeNumbers, int employeeCount, int employeeNumber) {
        for (int i = 0; i < employeeCount; i++) {
            if (employeeNumbers[i] == employeeNumber) {
                return i;
            }
        }
        return -1;
    }

    /** Converts time in HH:MM format into total minutes. */
    public static int convertTimeToMinutes(String time) {
        String[] timeParts = time.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);
        return (hour * 60) + minute;
    }

    /** Returns the name of the selected month. */
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

    //Computes SSS contribution using arrays.
    
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

    /** Computes the employee share of PhilHealth. */
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

    /** Computes the employee share of Pag-IBIG. */
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

    /** Computes withholding tax based on taxable income. */
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

    /** Computes total deductions. */
    public static double computeTotalDeductions(double monthlyGrossPay) {
        return computeSSS(monthlyGrossPay)
                + computePhilHealth(monthlyGrossPay)
                + computePagIbig(monthlyGrossPay)
                + computeIncomeTax(monthlyGrossPay);
    }
}