package com.example.ui.components;

import com.vaadin.flow.component.html.Div;

public class QRSvgHelper {

    /**
     * Generates an SVG representation of an Indonesian Standard QRIS QR Code.
     * Contains standard finder patterns, alignment patterns, and pseudo-random matrix cells based on data.
     */
    public static Div createQRISComponent(String payload, int sizePx) {
        Div container = new Div();
        container.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("background", "#FFFFFF")
                .set("padding", "16px")
                .set("border-radius", "16px")
                .set("border", "1px solid #E2E8F0")
                .set("box-shadow", "0 4px 16px rgba(15, 23, 42, 0.06)");

        int gridSize = 25; // 25x25 QR matrix
        boolean[][] matrix = new boolean[gridSize][gridSize];

        // Finder patterns (7x7) at top-left, top-right, bottom-left
        drawFinder(matrix, 0, 0);
        drawFinder(matrix, gridSize - 7, 0);
        drawFinder(matrix, 0, gridSize - 7);

        // Alignment pattern (5x5) at (16, 16)
        drawAlignment(matrix, gridSize - 9, gridSize - 9);

        // Timing patterns
        for (int i = 8; i < gridSize - 8; i++) {
            matrix[6][i] = (i % 2 == 0);
            matrix[i][6] = (i % 2 == 0);
        }

        // Fill payload deterministic data modules
        int hash = payload != null ? Math.abs(payload.hashCode()) : 123456789;
        long seed = hash * 6364136223846793005L + 1442695040888963407L;

        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                if (isReserved(r, c, gridSize)) continue;
                seed = (seed * 1103515245L + 12345L) & 0x7fffffffL;
                matrix[r][c] = (seed % 3 == 0 || (r + c + (seed % 5)) % 2 == 0);
            }
        }

        StringBuilder svg = new StringBuilder();
        int cellSize = 8;
        int svgSize = gridSize * cellSize;

        svg.append(String.format("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 %d %d' width='%d' height='%d'>",
                svgSize, svgSize, sizePx, sizePx));
        svg.append(String.format("<rect width='%d' height='%d' fill='#FFFFFF'/>", svgSize, svgSize));

        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                if (matrix[r][c]) {
                    svg.append(String.format("<rect x='%d' y='%d' width='%d' height='%d' fill='#0F172A' rx='1'/>",
                            c * cellSize, r * cellSize, cellSize, cellSize));
                }
            }
        }
        svg.append("</svg>");

        Div svgWrapper = new Div();
        svgWrapper.getElement().setProperty("innerHTML", svg.toString());
        container.add(svgWrapper);

        return container;
    }

    private static void drawFinder(boolean[][] matrix, int startX, int startY) {
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                if (r == 0 || r == 6 || c == 0 || c == 6) {
                    matrix[startY + r][startX + c] = true;
                } else if (r >= 2 && r <= 4 && c >= 2 && c <= 4) {
                    matrix[startY + r][startX + c] = true;
                } else {
                    matrix[startY + r][startX + c] = false;
                }
            }
        }
    }

    private static void drawAlignment(boolean[][] matrix, int startX, int startY) {
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                if (r == 0 || r == 4 || c == 0 || c == 4 || (r == 2 && c == 2)) {
                    matrix[startY + r][startX + c] = true;
                } else {
                    matrix[startY + r][startX + c] = false;
                }
            }
        }
    }

    private static boolean isReserved(int r, int c, int size) {
        // Top-left finder + separator
        if (r <= 7 && c <= 7) return true;
        // Top-right finder + separator
        if (r <= 7 && c >= size - 8) return true;
        // Bottom-left finder + separator
        if (r >= size - 8 && c <= 7) return true;
        // Alignment pattern
        if (r >= size - 9 && r <= size - 5 && c >= size - 9 && c <= size - 5) return true;
        // Timing lines
        if (r == 6 || c == 6) return true;
        return false;
    }
}
