package de.crass.poetradehelper.model;

public enum CurrencyID {
    ALTERATION(1),
    FUSING(2),
    ALCHEMY(3),
    CHAOS(4),
    GCP(5),
    EXALTED(6),
    CHROMATIC(7),
    JEWELLER(8),
    CHANCE(9),
    CHISEL(10),
    SCOURING(11),
    BLESSED(12),
    REGRET(13),
    REGAL(14),
    DIVINE(15),
    VAAL(16),
    SILVER(35),
    APPRENTICE(45),
    JOURNEYMAN(46),
    MASTER(47),
    NOT_SUPPORTED(-1);

    private final int id;

    CurrencyID(int ID) {
        id = ID;
    }

    public int getID() {
        return id;
    }

    public static CurrencyID get(int ID) {
        for (CurrencyID id : values()) {
            if (id.getID() == ID) {
                return id;
            }
        }
        return NOT_SUPPORTED;
    }

    @Override
    public String toString() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}