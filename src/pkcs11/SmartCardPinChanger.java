/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pkcs11;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Main class to the SmartCardPinChanger program. Handle user inputs and the
 * calls to the token manager
 *
 * @author IAM
 */
public class SmartCardPinChanger {

    /**
     * Entry poin to the Application
     * @param args Arguments
     */
    public static void main(String[] args) {
        //C:\Program Files\Gemalto\Classic Client\BIN\gclib.dll
        String dll = getDLLPath();
        TokenManager tokenManager = new TokenManager();
        if (tokenManager.initialize(dll)) {
            String[] pins = getinputPins();
            tokenManager.changeTokenPin(pins[0], pins[1]);
            tokenManager.release();
        }
    }

    /**
     * Get the DLL path from the user
     *
     * @return Path to the DLL to use
     */
    protected static String getDLLPath() {
        String dllPath = "";
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Please enter path to the DLL to use and press [Enter]:");
        try {
            dllPath = br.readLine();
        } catch (IOException ex) {
            Logger.getLogger(SmartCardPinChanger.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Impossible to read DLL path, exiting.");
            System.exit(0);
        }
        return dllPath;
    }

    /**
     * Get both actual and new desired PINs from the user.
     *
     * @return Pins
     */
    protected static String[] getinputPins() {
        String actualPin = "", newPin = "";
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            System.out.println("Please enter the actual PIN code and press [Enter]:");
            actualPin = br.readLine();
            System.out.println("Please enter the new PIN code and press [Enter]:");
            newPin = br.readLine();
        } catch (IOException ex) {
            Logger.getLogger(SmartCardPinChanger.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Impossible to read PIN code, exiting.");
            System.exit(0);
        }
        String[] result = {actualPin, newPin};
        return result;
    }

}
