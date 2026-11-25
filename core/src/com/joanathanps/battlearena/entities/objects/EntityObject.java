package com.joanathanps.battlearena.entities.objects;

import com.badlogic.gdx.graphics.Texture;
import com.joanathanps.battlearena.entities.Soldier;
import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.languages.Internationalization;
import com.joanathanps.battlearena.scenes.Match;

public abstract class EntityObject implements Comparable<EntityObject>{

    private Match match;
    private String name;
    private Texture icon;
    private ResourceHandler resources;
    private Internationalization i18n;
    private int tier;

    public EntityObject(Match match) {
        this.match = match;
        init();
    }

    private void init() {
        resources = match.getResources();
        i18n = match.getI18n();
    }

    public abstract boolean transformSoldier(Soldier soldier);

    public void setName(String name) {
        this.name = name;
    }

    public void setIcon(Texture icon) {
        this.icon = icon;
    }

    public Texture getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }

    public ResourceHandler getResources() {
        return resources;
    }

    public Internationalization getI18n() {
        return i18n;
    }

    public Match getMatch() {
        return match;
    }

    /** Higher is better. Used to decide whether a found item is worth swapping for a held one. */
    public int getTier() {
        return tier;
    }

    protected void setTier(int tier) {
        this.tier = tier;
    }

    /** Sorts best items first. */
    @Override
    public int compareTo(EntityObject o) {
        return Integer.compare(o.getTier(), this.getTier());
    }
}
