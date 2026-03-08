package motorphpayrollsystem;

import java.util.Scanner;

/**
 * MotorPHPayrollSystem.java
 *
 * This is the main entry point of the MotorPH Payroll System.
 * Displays the system menu and allows the user to access
 * different payroll features such as viewing employee
 * information, computing salaries, and computing net pay.
 *
 * Access rules:
 * - Options 1 to 3 are for payroll staff only.
 * - Option 4 is for employees who want to view their own details.
 * - Option 5 closes the program.
 */
public class MotorPHPayrollSystem {

    /**
     * Displays the main menu of the system.
     */
    public static void displayMenu() {
        System.out.println();
        System.out.println("==================================================");
        System.out.println("              MOTORPH PAYROLL SYSTEM              ");
        System.out.println("==================================================");
        System.out.println("1 - Calculate Hours Worked");
        System.out.println("2 - Compute Semi-Monthly Salary");
        System.out.println("3 - Compute Net Pay");
        System.out.println("4 - View Employee Details");
        System.out.println("5 - Exit");
        System.out.println("==================================================");
    }

    /**
     * Reads a whole-number input and checks if it is within the allowed range.
     *
     * @param input the Scanner object used for user input
     * @param prompt message shown before reading input
     * @param errorMessage message shown when input is outside the valid range
     * @param min lowest accepted value
     * @param max highest accepted value
     * @return a valid integer within the given range
     */
    public static int readValidIntInRange(Scanner input, String prompt, String errorMessage, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = input.nextLine().trim();

            try {
                int value = Integer.parseInt(line);

                // Accept the value only if it is inside the allowed range.
                if (value >= min && value <= max) {
                    return value;
                } else {
                    System.out.println(errorMessage);
                }
            } catch (NumberFormatException e) {
                // This happens when the user types text or a non-whole number.
                System.out.println("Invalid input. Please enter a whole number only.");
            }
        }
    }

    /**
     * Validates the payroll staff login before restricted features are opened.
     *
     * @param input the Scanner object used for user input
     * @return true if login is successful; otherwise false
     */
    public static boolean payrollStaffLogin(Scanner input) {
        System.out.println();
        System.out.println("--------------- PAYROLL STAFF LOGIN ---------------");
        System.out.println("Type 0 as username to go back to the main menu.");
        System.out.print("Enter username: ");
        String username = input.nextLine().trim();

        // Allow the user to cancel login and return to the menu.
        if (username.equals("0")) {
            System.out.println("Returning to main menu...");
            return false;
        }

        System.out.print("Enter password: ");
        String password = input.nextLine().trim();

        // Check the hard-coded payroll staff account credentials.
        if (username.equals("payroll_staff") && password.equals("12345")) {
            System.out.println("Login successful.");
            return true;
        } else {
            System.out.println("Invalid username or password.");
            return false;
        }
    }

    /**
     * Main method of the program.
     * It keeps showing the menu until the user selects Exit.
     */
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        int option = 0;

        // Keep looping until the user chooses option 5.
        while (option != 5) {
            displayMenu();

            option = readValidIntInRange(
                    input,
                    "Select option: ",
                    "Invalid option. Please select 1 to 5 only.",
                    1,
                    5
            );

            switch (option) {
                case 1:
                    // Payroll staff login is required before opening the hours-worked module.
                    if (payrollStaffLogin(input)) {
                        System.out.println();
                        System.out.println("Opening CalculateHoursWorked...");
                        CalculateHoursWorked.run(input);
                    }
                    break;

                case 2:
                    // Payroll staff login is required before opening the salary module.
                    if (payrollStaffLogin(input)) {
                        System.out.println();
                        System.out.println("Opening ComputeSemiMonthlySalary...");
                        ComputeSemiMonthlySalary.run(input);
                    }
                    break;

                case 3:
                    // Payroll staff login is required before opening the net-pay module.
                    if (payrollStaffLogin(input)) {
                        System.out.println();
                        System.out.println("Opening ComputeNetPay...");
                        ComputeNetPay.run(input);
                    }
                    break;

                case 4:
                    // Employees can directly open their own details page.
                    System.out.println();
                    System.out.println("Opening ViewEmployeeDetails...");
                    ViewEmployeeDetails.run(input);
                    break;

                case 5:
                    // End the program.
                    System.out.println("Exiting MotorPH Payroll System.");
                    break;

                default:
                    // This block should not normally run because input is already validated.
                    System.out.println("Invalid option. Please select 1 to 5 only.");
            }
        }

        // Close the Scanner only when the whole program is about to end.
        input.close();
    }
}
