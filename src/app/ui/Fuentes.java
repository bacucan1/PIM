/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package app.ui;

import java.awt.Font;
import java.io.InputStream;

/**
 *
 * @author Eimy
 */
public class Fuentes {
    public static Font getOverpass(float tamaño, int estilo) {
        try {
            // ruta relativa al JAR
            InputStream is = Fuentes.class.getResourceAsStream("/fonts/Overpass-VariableFont_wght.ttf");
            Font font = Font.createFont(Font.TRUETYPE_FONT, is);
            return font.deriveFont(estilo, tamaño);
        } catch (Exception e) {
            e.printStackTrace();
            return new Font("SansSerif", estilo, (int) tamaño); // fallback
        }
    }
    
}
