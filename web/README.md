# Battle Royale Lite web portal

Static site for the game, deployed on Vercel at [battleroyale.joanathan.in](https://battleroyale.joanathan.in). No framework and no build step. Design notes and the reason for each choice are in [`/DESIGN.md`](../DESIGN.md).

## What is here
- `index.html`, `css/`, `js/`, `fonts/`, `img/`: the portal. Plain HTML, CSS and JavaScript. Fonts (Barlow, OFL) are self-hosted.
- `game.html`, `battlearena/`, `assets/`: the compiled game (GWT/WebGL). The portal embeds `game.html` in an iframe. Do not edit these by hand.
- `BattleRoyaleArena-1.0.jar`, `Launch-BattleRoyaleArena.bat`: the desktop build and its Windows launcher.
- Live leaderboard: read from the Supabase `match_results` table with the public anon key (`js/leaderboard.js`).

## Run it locally
Browsers block the game over `file://`. Serve this folder instead:

```bash
python3 -m http.server 8000   # then open http://localhost:8000
```

On Windows, double-click `Run-Local-Server.bat`.

## When the JAR changes
The download section shows the file size and SHA-256 of `BattleRoyaleArena-1.0.jar`. After rebuilding it, update both in `index.html`:

```bash
sha256sum BattleRoyaleArena-1.0.jar
stat -c %s BattleRoyaleArena-1.0.jar
```

## Deploy to Vercel
1. Import the repository at [vercel.com/new](https://vercel.com/new).
2. Set **Root Directory** to `web`.
3. Deploy, then add the domain `battleroyale.joanathan.in` under Settings, Domains, and point a CNAME for `battleroyale` at `cname.vercel-dns.com`.
