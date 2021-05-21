package com.javmarina.client.fx;

import com.javmarina.client.Client;
import com.javmarina.client.JamepadManager;
import com.javmarina.client.services.ControllerService;
import com.javmarina.client.services.DefaultJamepadService;
import com.javmarina.client.services.KeyboardService;
import com.javmarina.client.services.bot.DiscordService;
import com.javmarina.webrtc.WebRtcLoader;
import com.javmarina.webrtc.signaling.SessionId;
import dev.onvoid.webrtc.media.MediaDevices;
import dev.onvoid.webrtc.media.audio.AudioDevice;
import dev.onvoid.webrtc.media.audio.AudioDeviceModule;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


public class ClientFx extends Application {

    private static List<AudioDevice> AUDIO_DEVICES = new ArrayList<>(0);
    public static AudioDeviceModule deviceModule;

    static {
        WebRtcLoader.loadLibrary();
    }

    public static void main(final String[] args) {
        try {
            AUDIO_DEVICES = MediaDevices.getAudioRenderDevices();
            deviceModule = new AudioDeviceModule();
        } catch (final Exception ignored) {
        }
        launch(args);
    }

    @Override
    public void start(final Stage primaryStage) throws Exception {
        final FXMLLoader loader = new FXMLLoader(
                ClientFx.class.getResource("/view/client.fxml"));
        final GridPane page = loader.load();
        final Scene scene = new Scene(page);

        final ClientController clientController = loader.getController();

        final ArrayList<ControllerService> services = getAvailableServices();
        clientController.setControllerServices(services);
        clientController.setAudioOutputDevices(AUDIO_DEVICES);
        clientController.setButtonAction(() -> {
            final ControllerService service = clientController.getSelectedControllerService();
            final AudioDevice audioDevice = clientController.getSelectedAudioDevice();
            final SessionId sessionId = clientController.getSessionId();

            final ConnectionFrame connectionFrame = new ConnectionFrame(
                    service, sessionId, audioDevice, primaryStage::show
            );
            try {
                connectionFrame.show();
                primaryStage.hide();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        });

        primaryStage.setTitle("Client configuration");
        primaryStage.getIcons().add(new Image(getClass().getClassLoader().getResourceAsStream("icon.png")));
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        final Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setX(bounds.getWidth()/2 - primaryStage.getWidth());
        primaryStage.setY((bounds.getHeight() - primaryStage.getHeight()) / 2);
    }

    /**
     * Gets a list of available {@link ControllerService}. Keyboard and Discord bot are always
     * available. Additional {@link DefaultJamepadService} are added if there are connected controllers.
     * @return list of available services.
     */
    private static ArrayList<ControllerService> getAvailableServices() {
        final ArrayList<DefaultJamepadService> jamepadServiceList = JamepadManager.getAvailableJamepadServices();
        final ArrayList<ControllerService> allServices = new ArrayList<>(2 + jamepadServiceList.size());
        allServices.add(new KeyboardService());
        final String token = getDiscordToken();
        if (token != null) {
            allServices.add(new DiscordService(token));
        }
        allServices.addAll(jamepadServiceList);

        return allServices;
    }

    /**
     * Create a 'discord.properties' file inside /resources with a field called DiscordBotToken
     * @return the Discord token or null if not found
     */
    @Nullable
    private static String getDiscordToken() {
        try (final InputStream input
                     = Client.class.getClassLoader().getResourceAsStream("discord.properties")) {

            final Properties prop = new Properties();
            if (input == null) {
                // Unable to find discord.properties
                return null;
            }

            // load a properties file from class path, inside static method
            prop.load(input);

            //get the property value and print it out
            return prop.getProperty("DiscordBotToken", null);
        } catch (final IOException ex) {
            return null;
        }
    }
}
