/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Ryan Anderson
 *
 * this class stores configuration data for the project
 * 
 */
public class ConfigData {

    private static final int MAPSIZE = 10;
    private String filename = null;
    private Map<String,String> properties = null;

    public ConfigData() {
        Random rand = new Random();
        filename = "cs653proj" + rand.nextInt(999) + ".cfg";
        properties = new HashMap<String,String>(MAPSIZE);
    }

    public ConfigData( String filename ) {
        this.filename = filename;
        properties = new HashMap<String,String>(MAPSIZE);
    }

    public static ConfigData getInstance( String filename ) {
        ConfigData cfg = new ConfigData(filename);
        if( cfg.load() ) {
            return cfg;
        }
        return null;
    }

    // <editor-fold defaultstate="collapsed" desc="save(1)">
    public void save(final String filename) {
        PrintWriter file = null;
        try {
            file = new PrintWriter(new FileWriter(filename));
            Set<Entry<String,String>>entries = properties.entrySet();
            for( Entry<String,String>e : entries ) {
                file.append(e.getKey() + " ").append(e.getValue()).append("\n");
            }
            file.close();
        } catch (IOException ex) {
            System.out.println("Could not save config data: " + ex);
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="save(2)">
    public void save() {
        save(filename);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="load(1)">
    public boolean load(String filename) {
        FileReader file = null;
        Pattern pattern = Pattern.compile("(\\w+)[ \\t]+([^\\n\\x0B\\f\\r]+)");
        try {
            file = new FileReader(new File(filename));
            BufferedReader reader = new BufferedReader(file);
            String line = reader.readLine();
            properties = new HashMap<String,String>(MAPSIZE);
            while (null != line) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    properties.put(matcher.group(1).trim(), matcher.group(2));
                }
                line = reader.readLine();
            }
            this.filename = filename;
            return true;
        } catch (IOException ex) {
            System.out.println("Could not load config data: " + ex);
            return false;
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="load(2)">
    public boolean load() {
        return load(filename);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Default get/set Methods">
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="addProperty">
    public boolean addProperty(String property, String value) {
        if (properties.containsKey(property)) {
            return false;
        }
        properties.put(property, value);
        return true;
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="addOrSetProperty">
    public void addOrSetProperty(String property, String value) {
        if (null == property) {
            return;
        }
        properties.put(property, value);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="hasProperty">
    public boolean hasProperty(String property) {
        return properties.containsKey(property);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="getProperty">
    public String getProperty(String property) {
        return properties.get(property);
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="setProperty">
    public boolean setProperty(String property, String value) {
        if (properties.containsKey(property)) {
            properties.put(property, value);
            return true;
        }
        return false;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="toString">
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        Set<Entry<String, String>> entries = properties.entrySet();
        for (Entry<String, String> e : entries) {
            out.append(e.getKey()).append(" ").append(e.getValue()).append("\n");
        }
        return out.toString();
    }
    // </editor-fold>
}
