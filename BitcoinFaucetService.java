import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;

public class BitcoinFaucetService {
    private static final String FAUCET_API_URL = "https://faucet.triangleplatform.com/bitcoin/testnet";

    public void deposit(Address receivingAddress, Coin amount) throws Exception {
        // Construct the URL for the faucet API
        String apiUrl = FAUCET_API_URL + "?address=" + receivingAddress.toString() + "&amount=" + amount.value;

        try {
            // Create HTTP connection
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Check response code
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("Bitcoin deposit successful.");
            } else {
                System.err.println("Failed to deposit Bitcoin. Response code: " + responseCode);
            }

            connection.disconnect();
        } catch (IOException e) {
            System.err.println("Failed to deposit Bitcoin: " + e.getMessage());
            throw e;
        }
    }
}
