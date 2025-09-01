/*
 * Copyright (c) 2024, RuneLiteAI Team
 * All rights reserved.
 */
package net.runelite.client.plugins.runeliteai;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.Duration;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

class RuneliteAIOverlay extends OverlayPanel
{
    private final Client client;
    private final RuneliteAIPlugin plugin;
    private final RuneliteAIConfig config;
    
    @Inject
    private RuneliteAIOverlay(Client client, RuneliteAIPlugin plugin, RuneliteAIConfig config)
    {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        addMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "RuneLite AI overlay");
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        // Don't show overlay if disabled
        if (!config.showOverlay())
        {
            return null;
        }

        // Get plugin status
        AnalysisResults.PluginStatus status = plugin.getStatus();
        if (status == null)
        {
            return null;
        }

        // Clear previous components
        panelComponent.getChildren().clear();

        // Title with status color
        boolean isActive = status.getActive() != null ? status.getActive() : false;
        boolean isDatabaseConnected = status.getDatabaseConnected() != null ? status.getDatabaseConnected() : false;
        Color titleColor = isActive && isDatabaseConnected ? Color.GREEN : 
                          isActive ? Color.YELLOW : Color.RED;
        
        panelComponent.getChildren().add(TitleComponent.builder()
            .text("RuneLite AI")
            .color(titleColor)
            .build());

        // Runtime
        long runtimeSeconds = (System.currentTimeMillis() - plugin.getStartupTimestamp()) / 1000;
        String runtime = formatDuration(runtimeSeconds);
        panelComponent.getChildren().add(LineComponent.builder()
            .left("Runtime:")
            .right(runtime)
            .build());

        // Ticks processed
        int ticks = status.getTicksProcessed() != null ? status.getTicksProcessed() : 0;
        panelComponent.getChildren().add(LineComponent.builder()
            .left("Ticks:")
            .right(formatNumber(ticks))
            .build());

        // Database status
        boolean dbConnected = status.getDatabaseConnected() != null ? status.getDatabaseConnected() : false;
        String dbStatus = dbConnected ? "Connected" : "Disconnected";
        Color dbColor = dbConnected ? Color.GREEN : Color.RED;
        panelComponent.getChildren().add(LineComponent.builder()
            .left("Database:")
            .right(dbStatus)
            .rightColor(dbColor)
            .build());

        // Player name
        if (client.getLocalPlayer() != null && client.getLocalPlayer().getName() != null)
        {
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Player:")
                .right(client.getLocalPlayer().getName())
                .build());
        }

        // World tick count
        panelComponent.getChildren().add(LineComponent.builder()
            .left("World Tick:")
            .right(String.valueOf(client.getTickCount()))
            .build());

        // Average processing time
        long avgProcessingTime = status.getAverageProcessingTimeMs() != null ? status.getAverageProcessingTimeMs() : 0;
        Color processingColor = avgProcessingTime <= 2 ? Color.GREEN : 
                               avgProcessingTime <= 5 ? Color.YELLOW : Color.RED;
        panelComponent.getChildren().add(LineComponent.builder()
            .left("Avg Time:")
            .right(avgProcessingTime + "ms")
            .rightColor(processingColor)
            .build());

        // Session ID (if connected and detailed info enabled)
        if (config.showDetailedInfo() && status.getSessionId() != null)
        {
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Session:")
                .right("#" + status.getSessionId())
                .build());
        }

        // Estimated data points per tick (if detailed info enabled)
        if (config.showDetailedInfo())
        {
            int estimatedDataPoints = plugin.getEstimatedDataPointsPerTick();
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Data/Tick:")
                .right(formatNumber(estimatedDataPoints))
                .build());
        }

        return super.render(graphics);
    }

    private String formatDuration(long seconds)
    {
        if (seconds < 60)
        {
            return seconds + "s";
        }
        else if (seconds < 3600)
        {
            return String.format("%d:%02d", seconds / 60, seconds % 60);
        }
        else
        {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return String.format("%d:%02d:%02d", hours, minutes, seconds % 60);
        }
    }

    private String formatNumber(long number)
    {
        if (number < 1000)
        {
            return String.valueOf(number);
        }
        else if (number < 1000000)
        {
            return String.format("%.1fK", number / 1000.0);
        }
        else
        {
            return String.format("%.1fM", number / 1000000.0);
        }
    }
}