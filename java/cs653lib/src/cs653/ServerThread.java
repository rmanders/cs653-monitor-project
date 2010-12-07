/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653;

import java.net.Socket;
import org.apache.log4j.Logger;
import cs653.security.DiffieHellmanExchange;
import cs653.security.KarnCodec;
import java.math.BigInteger;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Ryan Anderson
 *
 * A server thread that handles an individual incoming connection.
 *
 * <p>
 * Our server thread is kind of mean. If at any point of a transfer request
 * it doesn't like what it sees, it'll just return and drop the connection
 * instead of responding with a transfer declined (unless it is ourselves
 * who is requesting the transaction). If we REALLY wanted to be mean, we could
 * deliberately hold this connection open but do nothing for as long as it takes
 * the monitor to timeout. This will cause the attacker's client to waste their
 * monitor connection time.
 *</p>
 *
 */
public class ServerThread extends CommandInterpreter implements Runnable {

    private int threadId;
    private Thread runner;

    // <editor-fold defaultstate="collapsed" desc="Constructor">
    public ServerThread(Socket socConnection, int threadId, ConfigData config) {
        super(config, Logger.getLogger("ServerThread ID[" + threadId + "]"));
        this.threadId = threadId;
        this.socConnection = socConnection;
        if (!initConnectionIO()) {
            logger.error("FATAL ERROR: Failed to open IO buffers for connection");
            System.exit(1);
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="startServerThread">
    /**
     * Starts the ServerThread execution
     */
    public void startServertThread() {
        if (null == runner) {
            runner = new Thread(this);
            runner.start();
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="doLoginHandshake">
    /**
     * This method executes the protocol for a login handshake with the monitor.
     *
     * @return true if the handshake was successful, false otherwise
     */
    public boolean doLoginHandshake() {
        
        // Receive the first message group
        MessageGroup msgs = receiveMessageGroup();

        // First check for password checksum
        Directive dir =
                msgs.getNext(DirectiveType.PARTICIPANT_PASSWORD_CHECKSUM);
        if( !checkDirective(dir,DirectiveType.PARTICIPANT_PASSWORD_CHECKSUM) ) {
            return false;
        }

        String password = CONFIG.getProperty("password");
        if( !KarnCodec.quickSha(password).equals(dir.getArg(0)) ) {
            logger.warn(" The Participant Password Checksum from the foreign "
                    + "connection did not match. Rejecting login. Mine:[" +
                    password + "] theirs:[" + dir.getArg(0) + "]");
            return false;
        }

        // Expect Ident directive
        msgs.reset();
        dir = msgs.getNext(DirectiveType.REQUIRE);
        boolean result;

        if( null == dir ) {
            logger.error("Expected a REQUIRE directive but none found in "
                    + "message group.");
            return false;
        }
        if (!dir.getArg().equals("IDENT")) {
            logger.error("Local Server handshake failed: Expected IDENT, got "
                    + dir.getArg() );
            return false;
        }
        // Execute ident command
        // Setup Encryption if it's turned on and execute the IDENT command
        if( ENCRYPTION_ON ) {
            dhe = DiffieHellmanExchange.getInstance();
            String myPublicKey = dhe.getPublicKey().toString(32);
            result = executeCommand(Command.IDENT, identity, myPublicKey);
        } else {
            result = executeCommand(Command.IDENT);
        }
        if (!result) {
            logger.error("In doLoginHandshake: Failed to execute IDENT cmd");
            return false;
        }

        // Expect Alive directive
        msgs = receiveMessageGroup();
        dir = msgs.getNext(DirectiveType.REQUIRE);
        if (!dir.getArg().equals("ALIVE")) {
            logger.error("Local Server handshake failed: Expected ALIVE, got "
                    + dir.getArg());
            return false;
        }

        // Execute ALIVE command
        result = executeCommand(Command.ALIVE);
        if (!result) {
            logger.error("In doLoginHandshake: Failed to execute ALIVE command");
            return false;
        }

        // check for TRANSFER REQUEST
        msgs = receiveMessageGroup();

        if(msgs.hasDirective(DirectiveType.TRANSFER)) {
            logger.info("Transfer request detected during login handshake.");
            return handleTransfer(msgs);
        } else {
            msgs.reset();
        }

        // Expect QUIT        
        dir = msgs.getNext(DirectiveType.REQUIRE);
        if (!dir.getArg().equals("QUIT")) {
            logger.error("Local Server handshake failed: Expected QUIT, got "
                    + dir.getArg());
            return false;
        }

        // Execute QUIT command
        result = executeCommand(Command.QUIT);
        if (!result) {
            logger.error("In doLoginHandshake: Failed to execute QUIT command");
            return false;
        }

        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="transfer">
    /**
     *
     * Implements the transfer protocol and Zero-Knowledge proof authentication.
     * If all three servers and all of our clients are running on the same
     * machine, we can also use a lock file for added security.
     *
     * For now I assume everything is on the same machine so if you're testing
     * in any kind of different setup, you better modify this code!
     *
     * @param msgs {@link MessageGroup} containing latest monitor messages
     *
     * @return True if the transfer was accepted, false otherwise.
     */
    protected boolean handleTransfer(MessageGroup msgs) {

        // Ok folks, we now have a transfer request.
        msgs.reset();
        Directive dir = msgs.getNext(DirectiveType.TRANSFER);
        logger.info("Transfer request received: " + dir);

        // Expect public key in existing message group
        dir = msgs.getNext(DirectiveType.RESULT);
        if (!checkDirective(dir, DirectiveType.RESULT, "PUBLIC_KEY")) {
            return false;
        }

        // Store the initiators public key locally
        String keys[] = dir.getArgStringArray(1);
        if (null == keys || keys.length != 2) {
            logger.error("Error while retrieving public keys from initiator's "
                    + "RESULT PUBLIC_KEYS directive: " + dir);
            return false;
        }
        BigInteger v = new BigInteger(keys[0], 10);
        BigInteger n = new BigInteger(keys[1], 10);
        logger.debug("Got keys: " + v + ", " + n);

        // TODO: Maybe add certificate stuff here, but this will eat up network
        // time. Don't worry about it if we use the lock file. We need xfers to
        // be fast as we can get em so we don't time out before we need to.

        // Execute Rounds
        boolean result = executeCommand(Command.ROUNDS,
                String.valueOf(ZKP_MIN_ROUNDS));
        if (!result) {
            logger.error("Failed to execute command: " + Command.ROUNDS);
            return false;
        }

        // Expect AUTHORIZE_SET
        msgs = receiveMessageGroup();
        dir = msgs.getNext(DirectiveType.RESULT);
        if (!checkDirective(dir, DirectiveType.RESULT, "AUTHORIZE_SET")) {
            return false;
        }

        BigInteger authSet[] = dir.getArgBigIntegerArray(1);
        if (null == authSet || authSet.length < ZKP_MIN_ROUNDS) {
            logger.error("AuthorizeSet Results not formated propertly:" + dir);
        }

        // Expect REQUIRE SUBSET_A
        dir = msgs.getNext(DirectiveType.REQUIRE);
        if (!checkDirective(dir, DirectiveType.RESULT, "SUBSET_A")) {
            return false;
        }

        // Compute SUBSET_A
        StringBuilder fodder = new StringBuilder();
        List<Integer> subsetA = new LinkedList<Integer>();
        Random rand = new Random();

        for (int i = 0; i < ZKP_MIN_ROUNDS; i++) {
            if (rand.nextBoolean()) {
                subsetA.add(i);
                fodder.append(" ").append(i);
            }
        }

        // Execute SUBSET_A
        result = executeCommand(Command.SUBSET_A, fodder.toString());
        if (!result) {
            logger.error("Failed to execute command: " + Command.SUBSET_A);
            return false;
        }

        // Expect SUBSET_K
        msgs = receiveMessageGroup();
        dir = msgs.getFirstDirectiveOf(DirectiveType.RESULT, "SUBSET_K");
        if (!checkDirective(dir, DirectiveType.RESULT, "SUBSET_K")) {
            return false;
        }

        BigInteger subsetK[] = dir.getArgBigIntegerArray(1);
        if (null == subsetK || subsetK.length != subsetA.size()) {
            logger.error("Unexpected format or number of elements in subsetK: "
                    + dir);
            return false;
        }

        // Verify subsetK (yes, i know this is ugly... but I am short on time,
        // if you don't like it... feel free to contribute and change it
        // yourself)
        result = true;
        int j = 0;
        for (int i : subsetA) {
            if (!subsetK[j++].equals(authSet[i].multiply(v).mod(n))) {
                result = false;
                break;
            }
        }

        if (!result) {
            logger.warn("Invalid subsetK from initiator. Dropping connection..");
            return false;
        }

        // Expect SUBSET_J
        dir = msgs.getFirstDirectiveOf(DirectiveType.RESULT, "SUBSET_J");
        if (!checkDirective(dir, DirectiveType.RESULT, "SUBSET_J")) {
            return false;
        }

        BigInteger subsetJ[] = dir.getArgBigIntegerArray(1);

        // Same as before with subset_k
        result = true;
        j = 0;
        for (int i : subsetA) {
            if (!subsetJ[j++].equals(authSet[i])) {
                result = false;
                break;
            }
        }

        //boolean transferOk = isLockOpen(); // (if only want to use lockfile)
        boolean transferOk = isLockOpen() && result;
        logger.debug("Transfer OK?: " + transferOk);
        logger.debug("Set lock to NO succeded?: " + setLockNo());

        // Expect transfer response
        dir = msgs.getNext(DirectiveType.REQUIRE);
        if (!checkDirective(dir, DirectiveType.REQUIRE, "TRANSFER_RESPONSE")) {
            return false;
        }

        //Execute Transfer Response
        if (transferOk) {
            result = executeCommand(Command.TRANSFER_RESPONSE, "ACCEPT");
            logger.warn("(!) Transfer Request was ACCEPTED");
        } else {
            result = executeCommand(Command.TRANSFER_RESPONSE, "DECLINE");
            logger.warn("Transfer Request was DECLINED");
        }
        if (!result) {
            logger.error("Failed to execute command: "
                    + Command.TRANSFER_RESPONSE);
            return false;
        }

        // Expect QUIT
        msgs = receiveMessageGroup();
        dir = msgs.getNext(DirectiveType.REQUIRE);
        if (!checkDirective(dir, DirectiveType.REQUIRE, "QUIT")) {
            return false;
        }

        // Execute QUIT command
        result = executeCommand(Command.QUIT);
        if (!result) {
            logger.error("Failed to execute command: " + Command.QUIT);
            return false;
        }

        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="run">
    public void run() {

        if (!doLoginHandshake()) {
            logger.error("ServerThread Login handshake FAILED");
        }
        logger.debug("Server Thread Login handshake SUCCEEDED");

        logger.debug("Exiting Local Server thread");
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="checkDirective(1)">
    /**
     * Use this to check whether or not a directive matches an expected
     * directive and if not, output the appropriate error/debug statements to
     * the log files.
     *
     * @param dir A {@link Directive} to validate
     * @param expType The expected {@link DirectiveType}
     * @param expArg0 The expected first argument of the directive
     * @return true if the directive meets expectations, false otherwise
     *
     */
    private boolean checkDirective(Directive dir, DirectiveType expType,
            String expArg0) {
        if (null == dir) {
            logger.error("Expected a " + expType + " directive but none found "
                    + "in message group.");
            return false;
        }
        if (null != expArg0 && !dir.getArg().equals(expArg0)) {
            logger.error("Local Server handshake failed: Expected " + expArg0
                    + " got " + dir.getArg());
            return false;
        }
        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="checkDirective(2)">
    /**
     *
     * Use this to check whether or not a directive matches an expected
     * directive and if not, output the appropriate error/debug statements to
     * the log files.

     * @param dir A {@link Directive} to validate
     * @param expType The expected {@link DirectiveType}
     * @return true if the directive meets expectations, false otherwise
     *
     */
    private boolean checkDirective(Directive dir, DirectiveType expType) {
        return checkDirective(dir, expType, null);
    }
    // </editor-fold>

}
