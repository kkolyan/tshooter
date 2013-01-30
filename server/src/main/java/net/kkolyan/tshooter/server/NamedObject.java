package net.kkolyan.tshooter.server;

public final class NamedObject {
    private final String name;

    public NamedObject(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "NamedObject{" + name + '}';
    }
}
