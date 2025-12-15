/*
 * Copyright (C) 2025 Johan Andersson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package gui;

public class Firework {
    float x, y, vx, vy, age, maxAge;

    public Firework(float x, float y) {
        this.x = x;
        this.y = y;
        this.vx = (float)(Math.random() * 4 - 2);
        this.vy = (float)(Math.random() * 4 - 2);
        this.maxAge = 60 + (float)(Math.random() * 60);
        this.age = 0;
    }

    public void update() {
        x += vx;
        y += vy;
        age++;
        vx *= 0.98f;
        vy *= 0.98f;
    }

    public boolean isDead() {
        return age >= maxAge;
    }

    public float getAlpha() {
        return 1 - (age / maxAge);
    }
}