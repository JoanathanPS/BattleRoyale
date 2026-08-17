package com.joanathanps.battlearena.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.joanathanps.battlearena.graphics.AnimationRegion;
import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.scenes.Match;

import static com.joanathanps.battlearena.scheme.PhysicsAdapter.pScale;

public abstract class Entity extends Sprite {

    private Match match;
    private Body body;
    private Vector2 position;

    Entity(Match match, Vector2 position, AnimationRegion animationRegion, ResourceHandler.TextureAtlasPath textureAtlasPath) {
        super(match.getResources().getTextureAtlas(textureAtlasPath).findRegion(animationRegion.getRegion()));
        this.match = match;
        this.position = new Vector2(pScale(position.x), pScale(position.y));
    }

    protected abstract void buildBody();

    void setBody(Body body) {
        this.body = body;
    }

    public Match getMatch() {
        return match;
    }

    public Body getBody() {
        return body;
    }

    Vector2 getPosition() {
        return position;
    }
}
