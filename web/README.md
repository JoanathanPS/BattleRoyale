# Battle Royale Arena Web Portal

Web platform for **Battle Royale Arena** deployed at [battleroyale.joanathan.in](https://battleroyale.joanathan.in).

## Features
- **In-Browser WebAssembly JVM**: Powered by CheerpJ 3.0 to load and run `BattleRoyaleArena-1.0.jar` directly inside client browsers.
- **Live Supabase Leaderboard**: Real-time PostgREST sync with the game's Supabase match database (`https://ngssqooaxzaqobmowfrk.supabase.co`).
- **1-Click Release Download Hub**: Direct download for the 15.4 MB executable JAR (`BattleRoyaleArena-1.0.jar`) and companion launcher (`Launch-BattleRoyaleArena.bat`).
- **Interactive Guide & Field Manual**: Full controls, inventory switching, and safe zone shrinking mechanic guides.

## Vercel Deployment Instructions

### Option 1: Deploy with Vercel CLI
From the project root:
```bash
# Navigate to web directory
cd web

# Deploy to Vercel
vercel
```

When prompted:
- Set project name: `battleroyale`
- In Vercel Project Settings &rarr; **Domains**, add: `battleroyale.joanathan.in`.

### Option 2: Deploy via GitHub + Vercel Dashboard
1. Push this repository to GitHub: `https://github.com/JoanathanPS/BattleRoyale`.
2. In the [Vercel Dashboard](https://vercel.com/new), import the `BattleRoyale` repo.
3. Under **Root Directory**, click "Edit" and choose:
   ```
   web
   ```
4. Click **Deploy**.
5. Once deployed, go to **Settings &rarr; Domains** and add:
   ```
   battleroyale.joanathan.in
   ```
6. Add the CNAME DNS record in your DNS provider for `battleroyale` pointing to `cname.vercel-dns.com`.

