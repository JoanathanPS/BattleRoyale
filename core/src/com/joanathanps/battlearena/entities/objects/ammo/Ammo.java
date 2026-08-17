package com.joanathanps.battlearena.entities.objects.ammo;

import com.joanathanps.battlearena.entities.Soldier;
import com.joanathanps.battlearena.entities.objects.EntityObject;
import com.joanathanps.battlearena.scenes.Match;

public abstract class Ammo extends EntityObject {

    private int amount;

    public Ammo(Match match) {
        super(match);
    }

    public abstract void updateName();

    @Override
    public boolean transformSoldier(Soldier soldier) {
        return false;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void useAmount(int amount) {
        this.amount -= amount;
    }

    public int getAmount() {
        return amount;
    }

    public void useAll() {
        this.amount = 0;
    }
}
