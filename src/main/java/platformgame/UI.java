package platformgame;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import platformgame.Objects.Obj_Key;

import java.util.Objects;

public class UI {
    Game game;
    GraphicsContext gc;

    private Image keyIcon;
    private Image ammoIcon;
    private Image dialogueBoxImage;
    private Image parchmentBoxImage;
    private Image exclamationIcon;
    private Image cancelButtonImage;

    private Font dialogueFont;

    public boolean messageOn = false;
    public String message = "";
    public int msgCounter = 0;
    public String dialogue = "";

    private boolean dialogueAnimating = false;
    private double animationProgress = 0.0;
    private final double ANIMATION_SPEED = 0.08;
    private final double BOUNCE_STRENGTH = 0.15;
    private double dialogueScale = 0.0;
    private double dialogueOpacity = 0.0;
    private int previousGameState = -1;

    // ✅ Health UI images
    private Image heartIcon;
    private Image healthBarFull;
    private Image healthBarEmpty;

    public UI(Game game) {
        this.game = game;
        try {
            Obj_Key key = new Obj_Key();
            keyIcon = key.image;

            ammoIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream(
                    "/image/Object/Ammo.png")));

            dialogueBoxImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream(
                    "/image/00_UI/dialogue/UI_board_Large_stone.png")));
            parchmentBoxImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream(
                    "/image/00_UI/dialogue/UI_board_Large_parchment.png")));
            exclamationIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream(
                    "/image/00_UI/dialogue/Exclamation_Yellow.png")));
            cancelButtonImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream(
                    "/image/00_UI/dialogue/TextBTN_Cancel.png")));

            heartIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream(
                    "/image/Object/Health-Icon.png")));
            healthBarFull = new Image(Objects.requireNonNull(getClass().getResourceAsStream(
                    "/image/Object/Health-Bar.png")));
            healthBarEmpty = new Image(Objects.requireNonNull(getClass().getResourceAsStream(
                    "/image/Object/Health-line.png")));

            Font baseFont = Font.loadFont(
                    Objects.requireNonNull(getClass().getResourceAsStream(
                            "/font/SangSangFlowerRoadRegular.otf")), 26);
            dialogueFont = Font.font(baseFont.getFamily(), FontWeight.BOLD, 26);
        } catch (Exception e) {
            System.out.println("Resource loading error: " + e.getMessage());
            dialogueFont = Font.font("Arial", FontWeight.BOLD, 26);
        }
    }

    public void showMessage(String text) {
        message = text;
        messageOn = true;
    }

    public void startDialogueAnimation() {
        dialogueAnimating = true;
        animationProgress = 0.0;
        dialogueScale = 0.0;
        dialogueOpacity = 0.0;
    }



    public void drawHealthBar(GraphicsContext gc, int currentHP, int maxHP) {
        double heartX = 20;
        double heartY = 70;
        double heartSize = 35;

        double barX = heartX + heartSize;
        double frameHeight = 22;
        double barY = heartY + (heartSize - frameHeight) / 2.0;

        double frameWidth = 180;

        double paddingLeft = 1;
        double paddingTop = 2.5;

        double fillX = barX + paddingLeft;
        double fillY = barY + paddingTop;
        double fillWidth = frameWidth - paddingLeft * 2;
        double fillHeight = frameHeight - paddingTop * 2;

        double ratio = Math.max(0, Math.min(1.0, (double) currentHP / maxHP));
        double redBarDrawWidth = fillWidth * ratio;

        if (healthBarFull != null && ratio > 0) {
            gc.drawImage(
                    healthBarFull,
                    0, 0,
                    healthBarFull.getWidth() * ratio, healthBarFull.getHeight(),
                    fillX, fillY,
                    redBarDrawWidth, fillHeight
            );
        }

        if (healthBarEmpty != null) {
            gc.drawImage(
                    healthBarEmpty,
                    barX, barY,
                    frameWidth, frameHeight
            );
        }

        if (heartIcon != null) {
            gc.drawImage(heartIcon, heartX, heartY, heartSize, heartSize);
        }
    }

    private void updateDialogueAnimation() {
        if (dialogueAnimating) {
            animationProgress += ANIMATION_SPEED;

            if (animationProgress >= 1.0) {
                animationProgress = 1.0;
                dialogueAnimating = false;
                dialogueScale = 1.0;
                dialogueOpacity = 1.0;
            } else {
                double t = animationProgress;
                double bounce = Math.sin(t * Math.PI * 2) * BOUNCE_STRENGTH * (1 - t);
                dialogueScale = easeOutBack(t) + bounce;
                dialogueOpacity = easeOutQuart(t);
            }
        } else if (game.GameState == game.dialogueState) {
            dialogueScale = 1.0;
            dialogueOpacity = 1.0;
        }
    }

    private double easeOutBack(double t) {
        double c1 = 1.70158;
        double c3 = c1 + 1;
        return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
    }

    private double easeOutQuart(double t) {
        return 1 - Math.pow(1 - t, 4);
    }

    public void draw(GraphicsContext gc) {
        this.gc = gc;
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 24));

        if (game.GameState == game.dialogueState && previousGameState != game.dialogueState) {
            startDialogueAnimation();
        }
        previousGameState = game.GameState;

        if (game.GameState == game.playState) {
            if (keyIcon != null) {
                gc.drawImage(keyIcon, 20, 20, 32, 32);
            }


            gc.fillText("x " + game.hasKey, 60, 45);

            // ✅ Draw Ammo
            if (ammoIcon != null) {
                gc.drawImage(ammoIcon, 110, 20, 32, 32);
                gc.fillText("x " + game.player.ammo, 150, 45);
            }

            drawHealthBar(gc, game.player.hp, game.player.maxHp);
            if (messageOn) {
                gc.setFont(Font.font("Arial", 30));
                gc.fillText(message, game.tileSize / 2, game.tileSize * 5);
                msgCounter++;
                if (msgCounter > 120) {
                    message = "";
                    messageOn = false;
                    msgCounter = 0;
                }
            }
        }

        if (game.GameState == game.dialogueState) {
            updateDialogueAnimation();
            drawDialogueScreen();
        }
    }

    public void drawDialogueScreen() {
        gc.save();

        int boxWidth = (int) (game.screenWidth * 0.8);
        int estimatedLines = Math.max(3, dialogue.length() / 40);
        int boxHeight = (int) (estimatedLines * 32 + 80);
        int boxX = (int) ((game.screenWidth - boxWidth) / 2);
        int boxY = game.tileSize + 50;

        double centerX = boxX + boxWidth / 2.0;
        double centerY = boxY + boxHeight / 2.0;

        gc.setGlobalAlpha(dialogueOpacity);
        gc.translate(centerX, centerY);
        gc.scale(dialogueScale, dialogueScale);
        gc.translate(-centerX, -centerY);

        if (dialogueAnimating) {
            gc.setFill(Color.rgb(0, 0, 0, 0.3 * dialogueOpacity));
            gc.fillRoundRect(boxX + 5, boxY + 5, boxWidth + 20, boxHeight + 30, 10, 10);
        }

        if (dialogueBoxImage != null) {
            gc.drawImage(dialogueBoxImage, boxX - 10, boxY - 15, boxWidth + 20, boxHeight + 30);
        }

        if (parchmentBoxImage != null) {
            gc.drawImage(parchmentBoxImage, boxX + 5, boxY + 3, boxWidth - 10, boxHeight);
        }

        if (exclamationIcon != null) {
            double iconSize = 32;
            double iconX = boxX + (boxWidth - iconSize) / 2;
            double iconY = boxY - iconSize - 8;

            double iconBounce = dialogueAnimating ? Math.sin(animationProgress * Math.PI * 3) * 5 * (1 - animationProgress) : 0;
            iconY += iconBounce;

            gc.setFill(Color.rgb(218, 165, 32));
            gc.fillOval(iconX - 6, iconY - 6, iconSize + 12, iconSize + 12);

            gc.setStroke(Color.rgb(139, 69, 19));
            gc.setLineWidth(3);
            gc.strokeOval(iconX - 6, iconY - 6, iconSize + 12, iconSize + 12);

            gc.drawImage(exclamationIcon, iconX, iconY, iconSize, iconSize);
        }

        gc.setFont(dialogueFont != null ? dialogueFont : Font.font("Arial", FontWeight.BOLD, 24));
        gc.setFill(Color.rgb(61, 43, 31));

        double textX = boxX + boxWidth * 0.1;
        double textY = boxY + 40;
        double textWidth = boxWidth * 0.8;

        drawWrappedText(dialogue, textX, textY, textWidth);

        gc.restore();
    }

    private void drawWrappedText(String text, double x, double y, double maxWidth) {
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        double lineHeight = gc.getFont().getSize() + 6;
        double currentY = y;

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            double estimatedWidth = gc.getFont().getSize() * testLine.length() * 0.55;

            if (estimatedWidth > maxWidth && currentLine.length() > 0) {
                gc.fillText(currentLine.toString(), x, currentY);
                currentLine = new StringBuilder(word);
                currentY += lineHeight;
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }

        if (currentLine.length() > 0) {
            gc.fillText(currentLine.toString(), x, currentY);
        }
    }

    public void subWindows(int x, int y, int height, int width) {
        Color fillColor = Color.rgb(0, 0, 0, 0.85);
        Color borderColor = Color.rgb(255, 255, 255);

        gc.setFill(fillColor);
        gc.fillRoundRect(x, y, width, height, 35, 35);

        gc.setStroke(borderColor);
        gc.setLineWidth(5);
        gc.strokeRoundRect(x + 5, y + 5, width - 10, height - 10, 25, 25);
    }

    public void drawPauseScreen() {
        // For future use
    }
}
