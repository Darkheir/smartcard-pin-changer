/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pkcs11;

import iaik.pkcs.pkcs11.Module;
import iaik.pkcs.pkcs11.Session;
import iaik.pkcs.pkcs11.Slot;
import iaik.pkcs.pkcs11.Token;
import iaik.pkcs.pkcs11.TokenException;
import iaik.pkcs.pkcs11.TokenInfo;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handle all the PKCS11 related operations in the program.
 *
 * @author IAM
 */
public class TokenManager {

    private Module pkcs11Module;
    private Token token;

    public TokenManager() {

    }

    /**
     * Get the available token slots
     *
     * @return Array of the token slots availables
     * @throws TokenException
     */
    public Slot[] getTokenSlots() throws TokenException {
        return pkcs11Module.getSlotList(Module.SlotRequirement.TOKEN_PRESENT);
    }

    /**
     * Load the token to use. For now the token in the first available slot is
     * used
     *
     * @return Succes of the operation
     * @throws TokenException
     */
    protected boolean loadToken() throws TokenException {
        Slot[] slots = this.getTokenSlots();
        if (slots.length == 0) { //No tokens connected  
            System.out.println("Couldn't find any token.");
            return false;
        }
        System.out.println(slots.length + " slot(s) available.");
        this.token = slots[0].getToken();
        System.out.println("Connected to token");
        return true;
    }

    /**
     * Initialize the token manager: 
     * <ul>
     * <li>Initialize the PKCS#11 module</li>
     * <li>Set the Token to use</li>
     * </ul>
     * @param librarayPath
     * @return success of the operation
     */
    public boolean initialize(String librarayPath) {
        try {
            return initializePkcs11Module(librarayPath) && this.loadToken();
        } catch (TokenException ex) {
            Logger.getLogger(SmartCardPinChanger.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Impossible to initialize the token manager, exiting");
            System.exit(0);
        }
        return false;
    }

    /**
     * Initialize the PKCS#11 module
     *
     * @param librarayPath Path to the cryptoki DLL
     * @return Success of the operation
     * @throws TokenException
     */
    protected boolean initializePkcs11Module(String librarayPath) throws TokenException {
        System.out.println("Initializing PKCS11 module ...");
        try {
            pkcs11Module = Module.getInstance(librarayPath);
            pkcs11Module.initialize(null);
            return true;
        } catch (IOException ex) {
            Logger.getLogger(SmartCardPinChanger.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Impossible to initialize the PKCS11 module: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Release context
     */
    public void release() {
        //CAtch le bordel
        System.out.println("Releasing token context");
        try {
            this.token.closeAllSessions();
            pkcs11Module.finalize(null);
        } catch (TokenException ex) {
            Logger.getLogger(SmartCardPinChanger.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Error while releasing context");
        }
    }

    /**
     * Perform the change PIN operation
     *
     * @param oldPin
     * @param newPin
     * @return Success of the operation
     */
    public boolean changeTokenPin(String oldPin, String newPin) {
        Session openedSession = this.openSession();
        if (openedSession == null) {
            return false;
        }
        if (!this.loginToToken(openedSession, oldPin)) {
            this.closeSession(openedSession);
            return false;
        }
        return this.setPin(openedSession, oldPin, newPin);
    }

    /**
     * Set the new PIN
     *
     * @param openedSession An opened Session
     * @param oldPin The actual Pin
     * @param newPin The new Pin
     * @return Success of the operation
     */
    protected boolean setPin(Session openedSession, String oldPin, String newPin) {
        try {
            openedSession.setPIN(oldPin.toCharArray(), newPin.toCharArray());
            System.out.println("PIN changed.");
            this.closeSession(openedSession);
            return true;
        } catch (TokenException ex) {
            Logger.getLogger(SmartCardPinChanger.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Impossible to set the new PIN");
            return false;
        }
    }

    /**
     * Open a new session on the token
     *
     * @return The opened session
     */
    protected Session openSession() {
        Session session;
        try {
            session = this.token.openSession(
                    Token.SessionType.SERIAL_SESSION,
                    Token.SessionReadWriteBehavior.RW_SESSION,
                    null,
                    null
            );
        } catch (TokenException ex) {
            Logger.getLogger(SmartCardPinChanger.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Impossible to open a session on the token");
            return null;
        }
        return session;
    }

    /**
     * Close the given session
     *
     * @param session
     */
    protected void closeSession(Session session) {
        try {
            session.closeSession();
        } catch (TokenException ex) {
            Logger.getLogger(SmartCardPinChanger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Log-in to the given session
     *
     * @param session Session to log to
     * @param userPINString PIN to access the token
     * @return Succes of the log-in
     */
    protected boolean loginToToken(Session session, String userPINString) {
        TokenInfo tokenInfo;
        try {
            tokenInfo = this.token.getTokenInfo();
            if (!tokenInfo.isLoginRequired()) {
                return true;
            }
            if (tokenInfo.isProtectedAuthenticationPath()) {
                System.out.println("Please enter the user PIN at the PIN-pad of your reader.");
                session.login(Session.UserType.USER, null); // the token prompts the PIN by other means; e.g. PIN-pad
                return true;
            }
            session.login(Session.UserType.USER, userPINString.toCharArray());
            return true;
        } catch (TokenException ex) {
            Logger.getLogger(SmartCardPinChanger.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Impossible to log-in the token. The PIN code may be wrong ...");
            return false;
        }
    }

}
