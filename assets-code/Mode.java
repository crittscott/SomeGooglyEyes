package com.github.crittscott.assets;

/** Entity-model extraction mode, selected via {@code /assets <mode>}. */
public enum Mode {
    SKELETON("skeleton", "json"),
    GEOMETRY("geometry", "json"),
    HEAD("head", "json"),
    BLOCKBENCH("blockbench", "bbmodel");

    /** Output sub-folder name, also used in chat messages. */
    public final String folder;
    /** Output file extension (without the dot). */
    public final String extension;

    Mode(String folder, String extension) {
        this.folder = folder;
        this.extension = extension;
    }
}
