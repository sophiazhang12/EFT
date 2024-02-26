import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.Wallet;
import java.io.File;



public class BankTest {
    public static void main(String[] args) {
        NetworkParameters params = TestNet3Params.get();
        testDepositBitcoinFromFaucet(params);
    }

    
    private static void testDepositBitcoinFromFaucet(NetworkParameters params) {
        WalletAppKit kit = new WalletAppKit(params, new File("."), "test_wallet");
        kit.startAsync();
        kit.awaitRunning();
    
        Wallet wallet = kit.wallet();
    
        // Create a test receiving address
        Address receivingAddress = wallet.freshReceiveAddress();
    
        // Create a test amount of Bitcoin to deposit
        Coin amountToDeposit = Coin.valueOf(500000); // Example: deposit 0.005 BTC
    
        // Create an instance of BitcoinFaucetService
        BitcoinFaucetService faucetService = new BitcoinFaucetService();
    
        // Test deposit functionality
        try {
            System.out.println("Attempting to deposit " + amountToDeposit.toFriendlyString() +
                               " to address: " + receivingAddress.toString());
            faucetService.deposit(receivingAddress, amountToDeposit);
            System.out.println("Deposit test successful.");
        } catch (Exception e) {
            System.err.println("Deposit test failed: " + e.getMessage());
        }
    
        kit.stopAsync();
        kit.awaitTerminated();
    }
    
    
}
