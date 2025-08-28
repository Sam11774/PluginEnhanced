package net.runelite.client.plugins.runeliteai;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.game.ItemManager;
import net.runelite.client.config.ConfigManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SimpleDataCollectionTest {

    @Mock
    private Client client;
    @Mock
    private ItemManager itemManager;
    @Mock
    private ConfigManager configManager;
    @Mock
    private Player localPlayer;
    @Mock
    private GameStateSnapshot gameStateSnapshot;
    @Mock
    private GameStateDelta gameStateDelta;

    private DataCollectionManager dataCollectionManager;

    @Before
    public void setUp() {
        // Minimal setup to debug the issue
        when(client.getLocalPlayer()).thenReturn(localPlayer);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(localPlayer.getWorldLocation()).thenReturn(new WorldPoint(3200, 3200, 0));
        when(gameStateSnapshot.getTimestamp()).thenReturn(System.currentTimeMillis());

        RuneliteAIPlugin mockPlugin = mock(RuneliteAIPlugin.class);
        when(mockPlugin.getAndResetKeyPressCount()).thenReturn(0);
        when(mockPlugin.getAndCleanRecentKeyPresses()).thenReturn(new java.util.concurrent.ConcurrentHashMap<>());
        dataCollectionManager = new DataCollectionManager(client, itemManager, configManager, mockPlugin);
    }

    @Test
    public void testBasicCollectionDebug() {
        System.out.println("=== Starting basic collection debug ===");
        try {
            TickDataCollection result = dataCollectionManager.collectAllData(123, 1000, gameStateSnapshot, gameStateDelta);
            System.out.println("Result: " + result);
            if (result != null) {
                System.out.println("Success! Result timestamp: " + result.getTimestamp());
            } else {
                System.out.println("Result is null - check for exceptions");
            }
        } catch (Exception e) {
            System.out.println("Exception caught: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("=== End debug ===");
    }
}