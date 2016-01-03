/*
 * PwnFilter -- Regex-based User Filter Plugin for Bukkit-based Minecraft servers.
 * Copyright (c) 2013 Pwn9.com. Tremor77 <admin@pwn9.com> & Sage905 <patrick@toal.ca>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */

package com.pwn9.PwnFilter.util;

import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * Unified Logging interface for PwnFilter-related messages
 * User: ptoal
 * Date: 13-10-02
 * Time: 4:50 PM
 *
 * @author ptoal
 * @version $Id: $Id
 */
public class LogManager {
    /** Constant <code>ruleLogLevel</code> */
    private static Level ruleLogLevel;
    // Logging variables
    /** Constant <code>debugMode</code> */
    public static DebugModes debugMode = DebugModes.off;
    /** Constant <code>logger</code> */
    private static Logger logger;
    private static File logFolder;

    private FileHandler logfileHandler;

    private static LogManager _instance;

    private LogManager() {
    }

    /**
     * <p>Getter for the field <code>ruleLogLevel</code>.</p>
     *
     * @return a {@link java.util.logging.Level} object.
     */
    public static Level getRuleLogLevel() {
        return ruleLogLevel;
    }

    /**
     * <p>Setter for the field <code>debugMode</code>.</p>
     *
     * @param mode a {@link java.lang.String} object.
     */
    public static void setDebugMode(String mode) {
        try {
            debugMode = LogManager.DebugModes.valueOf(mode);
        } catch (IllegalArgumentException e) {
            debugMode = LogManager.DebugModes.off;
        }
    }

    /**
     * <p>Setter for the field <code>ruleLogLevel</code>.</p>
     *
     * @param level a {@link java.lang.String} object.
     */
    public static void setRuleLogLevel(String level) {
        try {
            LogManager.ruleLogLevel = Level.parse(level.toUpperCase());
        } catch (IllegalArgumentException e ) {
            LogManager.ruleLogLevel = Level.INFO;
        }
    }

    /**
     * <p>debugLow.</p>
     *
     * @param message a {@link java.lang.String} object.
     */
    public void debugLow(String message) {
        if (debugMode.compareTo(DebugModes.low) >= 0) {
            logger.info(message);
        }
    }

    /**
     * <p>debugMedium.</p>
     *
     * @param message a {@link java.lang.String} object.
     */
    public void debugMedium(String message) {
        if (debugMode.compareTo(DebugModes.medium) >= 0) {
            logger.info(message);
        }
    }

    /**
     * <p>debugHigh.</p>
     *
     * @param message a {@link java.lang.String} object.
     */
    public void debugHigh(String message) {
        if (debugMode.compareTo(DebugModes.high) >= 0) {
            logger.info(message);
        }
    }

    /**
     * <p>getInstance.</p>
     *
     * @param l a {@link java.util.logging.Logger} object.
     * @param f a {@link java.io.File} object.
     * @return a {@link com.pwn9.PwnFilter.util.LogManager} object.
     */
    public static LogManager getInstance(Logger l, File f) {
        if (_instance == null) {
            _instance = new LogManager();
            logger = l;
            logFolder = f;
        }
        return _instance;
    }

    /**
     * <p>getInstance.</p>
     *
     * @return a {@link com.pwn9.PwnFilter.util.LogManager} object.
     */
    public static LogManager getInstance() {
        if (_instance == null) {
            throw new IllegalStateException("LogManager not yet initialized!");
        } else {
            return _instance;
        }
    }

    /**
     * <p>stop.</p>
     */
    public void stop() {
        if (logfileHandler != null) {
            logfileHandler.close();
            logfileHandler = null;
        }
    }

    public static void error(String s) {
        getInstance().logfileHandler.publish(new LogRecord(Level.SEVERE, s));
        logger.error(s);
    }

    public static void warn(String s) {
        getInstance().logfileHandler.publish(new LogRecord(Level.WARNING, s));
        logger.warn(s);
    }

    public static void info(String s) {
        getInstance().logfileHandler.publish(new LogRecord(Level.INFO, s));
        logger.info(s);
    }

    public static void log(String s) {
        getInstance().logfileHandler.publish(new LogRecord(ruleLogLevel, s));
        logger.info(s);
    }

    public enum DebugModes {
        off, // Off
        low, // Some debugging
        medium, // More debugging
        high, // You're crazy. :)
    }

    /**
     * <p>start.</p>
     */
    public void start() {
        if (logfileHandler == null) {
            try {
                // For now, one logfile, like the old way.
                String fileName =  new File(logFolder, "pwnfilter.log").toString();
                logfileHandler = new FileHandler(fileName, true);
                SimpleFormatter f = new PwnFormatter();
                logfileHandler.setFormatter(f);
                logfileHandler.setLevel(Level.FINEST); // Catch all log messages
                LogManager.logger.info("Now logging to " + fileName );

            } catch (IOException e) {
                LogManager.logger.error("Unable to open logfile.");
            } catch (SecurityException e) {
                LogManager.logger.error("Security Exception while trying to add file Handler");
            }
        }

    }

}
