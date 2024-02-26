import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.Level;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Bank {

    // Constants and Global Variables
    private static final String WALLET_FILE_NAME = "bitcoin-wallet";
    private static final String FEE_WALLET_FILE_NAME = "fee-wallet"; // New fee wallet file name
    private static final NetworkParameters params = TestNet3Params.get();
    private static final long RECENT_PERIOD = 24 * 60 * 60 * 1000; // 24 hours in milliseconds
    private static List<Transaction> recentTransactions = new ArrayList<>();
    private static WalletAppKit walletAppKit = null;
    private static WalletAppKit feeWalletAppKit = null; // New fee wallet

    static {
        // Assuming SLF4J is bound to logback
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.ERROR);
    }

    public static void main(String[] args) throws Exception {
        // Wallet setup
        Wallet wallet = checkOrCreateWallet(params); 

        // Fee wallet setup
        Wallet feeWallet = checkOrCreateFeeWallet(params); // Create or load fee wallet

        // Event listener for transactions
        wallet.addCoinsReceivedEventListener(new WalletCoinsReceivedEventListener() {
            @Override
            public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                long currentTime = System.currentTimeMillis();
                long transactionTime = tx.getUpdateTime().getTime();
                if (currentTime - transactionTime <= RECENT_PERIOD) {
                    recentTransactions.add(tx);
                    System.out.println("New recent transaction: " + tx.getHashAsString());
                }
            }
        });
        
        // Initial setup output
        printWalletAndConnectionInfo(wallet); 

        // Deposit Bitcoin from faucet
        depositBitcoinFromFaucet(wallet);

        // Continuous balance check loop
        while (true) {
            System.out.println("Wallet balance (in satoshis): " + wallet.getBalance().value);
            System.out.println("Block height: " + walletAppKit.chain().getBestChainHeight());
            System.out.println("Peers: " + walletAppKit.peerGroup().getConnectedPeers().size());

            // Optionally, clean up old transactions from the list
            long currentTime = System.currentTimeMillis();
            recentTransactions.removeIf(tx -> currentTime - tx.getUpdateTime().getTime() > RECENT_PERIOD);

            TimeUnit.SECONDS.sleep(30); // Adjust check interval as needed
        }
    }

    // Helper Functions
    private static Wallet checkOrCreateWallet(NetworkParameters params) throws IOException, UnreadableWalletException {
        walletAppKit = new WalletAppKit(params, new File("."), WALLET_FILE_NAME);
        walletAppKit.setBlockingStartup(false);
        walletAppKit.startAsync();
        walletAppKit.awaitRunning();        
        walletAppKit.peerGroup().setBloomFilterFalsePositiveRate(0.001); // Example: 0.1% false positive rate

        System.out.println("Wallet address: " + walletAppKit.wallet().currentReceiveAddress().toString());

        File walletFile = new File(WALLET_FILE_NAME + ".wallet");
        
        if (walletFile.exists()) {
            // Wallet exists, load it
            return Wallet.loadFromFile(walletFile);
        } else {
            Wallet wallet = walletAppKit.wallet();
            wallet.saveToFile(walletFile);
            throw new UnreadableWalletException("Wallet not found, created a new one");
        }
    }

    private static Wallet checkOrCreateFeeWallet(NetworkParameters params) {
        feeWalletAppKit = new WalletAppKit(params, new File("."), FEE_WALLET_FILE_NAME);
        feeWalletAppKit.setBlockingStartup(false);
        feeWalletAppKit.startAsync();
        feeWalletAppKit.awaitRunning();        
        feeWalletAppKit.peerGroup().setBloomFilterFalsePositiveRate(0.001); // Example: 0.1% false positive rate
    
        System.out.println("Fee Wallet address: " + feeWalletAppKit.wallet().currentReceiveAddress().toString());
    
        File feeWalletFile = new File(FEE_WALLET_FILE_NAME + ".wallet");
    
        try {
            if (feeWalletFile.exists()) {
                // Fee wallet exists, load it
                return Wallet.loadFromFile(feeWalletFile);
            } else {
                Wallet feeWallet = feeWalletAppKit.wallet();
                feeWallet.saveToFile(feeWalletFile);
                return feeWallet;
            }
        } catch (IOException | UnreadableWalletException e) {
            System.err.println("Error creating or loading fee wallet: " + e.getMessage());
            return null; // Return null if an exception occurs
        }
    }
    

    private static void printWalletAndConnectionInfo(Wallet wallet) {
        System.out.println("Initial Balance: " + wallet.getBalance().toFriendlyString());
        System.out.println("Network: " + params.getId());
        System.out.println("Connected peers: " + walletAppKit.peerGroup().getConnectedPeers().size());
        System.out.println("Wallet address: " + wallet.currentReceiveAddress().toString());
        System.out.println("Block height: " + walletAppKit.chain().getBestChainHeight());
    }

    private static void depositBitcoinFromFaucet(Wallet wallet) {
        // Replace this placeholder code with actual code to interact with a Bitcoin faucet service
        BitcoinFaucetService faucetService = new BitcoinFaucetService();
        Address receivingAddress = wallet.currentReceiveAddress();
        Coin amountToDeposit = Coin.valueOf(10_000); // 0.0001 BTC in Satoshis

    
        try {
            faucetService.deposit(receivingAddress, amountToDeposit);
            System.out.println("Successfully deposited Bitcoin from faucet to wallet.");
        } catch (Exception e) {
            System.err.println("Failed to deposit Bitcoin from faucet: " + e.getMessage());
        }
    }

    private static void withdrawBitcoinToFeeWallet(Wallet wallet, Wallet feeWallet) {
    // Withdraw Bitcoin from main wallet to fee wallet
    Coin feeAmount = Coin.valueOf(10000); // Example fee amount: 0.0001 BTC

    // Check if the main wallet has enough funds to cover the fee
    Coin totalAmount = feeAmount.add(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE); // Include transaction fee
    if (wallet.getBalance().compareTo(totalAmount) < 0) {
        System.err.println("Insufficient funds in the main wallet to cover the fee.");
        return;
    }

    // Proceed with the withdrawal
    Address feeAddress = feeWallet.currentReceiveAddress();
    SendRequest request = SendRequest.to(feeAddress, feeAmount);
    try {
        wallet.completeTx(request); // Complete the transaction
        wallet.commitTx(request.tx); // Commit the transaction to the network
        System.out.println("Successfully withdrawn Bitcoin to fee wallet. Fee deducted: " + feeAmount.toFriendlyString());
    } catch (InsufficientMoneyException e) {
        System.err.println("Insufficient funds in the main wallet to complete the transaction: " + e.getMessage());
    }
}

}
