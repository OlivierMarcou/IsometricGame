package net.arkaine.combat;

import net.arkaine.model.GameModel;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;

import java.util.*;

/**
 * Système de combat avec ennemis IA, caractéristiques RPG et projectiles
 */
public class CombatSystem {

    public enum DamageType {
        PHYSICAL, FIRE, ICE, POISON, LIGHTNING
    }

    public enum EnemyClass {
        WARRIOR, MAGE, ARCHER, ELITE_WARRIOR, ELITE_MAGE, ELITE_ARCHER, BOSS
    }

    public enum BehaviorType {
        PACK,      // Meute - attaquent en groupe
        SOLITARY,  // Solitaire - agit seul
        GUARDIAN   // Garde une zone spécifique
    }

    public static class Stats {
        public int health;
        public int maxHealth;
        public int damage;
        public double moveSpeed;
        public double attackSpeed;
        public double range;
        public Map<DamageType, Double> resistances;

        public Stats(int health, int damage, double moveSpeed, double attackSpeed, double range) {
            this.health = this.maxHealth = health;
            this.damage = damage;
            this.moveSpeed = moveSpeed;
            this.attackSpeed = attackSpeed;
            this.range = range;
            this.resistances = new HashMap<>();

            // Résistances par défaut
            for (DamageType type : DamageType.values()) {
                resistances.put(type, 0.0);
            }
        }

        public void setResistance(DamageType type, double resistance) {
            resistances.put(type, Math.max(0.0, Math.min(0.95, resistance))); // Max 95% résistance
        }

        public boolean isAlive() {
            return health > 0;
        }

        public double getHealthPercent() {
            return (double) health / maxHealth;
        }
    }

    public static class Projectile {
        public Point2D position;
        public Point2D target;
        public Point2D velocity;
        public DamageType damageType;
        public int damage;
        public double speed;
        public boolean active;
        public Color color;
        public Entity source;
        public double timeAlive;
        public static final double MAX_LIFETIME = 10.0; // 10 secondes max

        public Projectile(Point2D start, Point2D target, DamageType damageType, int damage, double speed, Entity source) {
            this.position = start;
            this.target = target;
            this.damageType = damageType;
            this.damage = damage;
            this.speed = speed;
            this.active = true;
            this.source = source;
            this.timeAlive = 0;

            // Calculer la vélocité
            double dx = target.getX() - start.getX();
            double dy = target.getY() - start.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance > 0) {
                this.velocity = new Point2D((dx / distance) * speed, (dy / distance) * speed);
            } else {
                this.velocity = new Point2D(0, 0);
            }

            // Couleur selon le type de dégât
            switch (damageType) {
                case FIRE: this.color = Color.ORANGE; break;
                case ICE: this.color = Color.LIGHTBLUE; break;
                case POISON: this.color = Color.GREEN; break;
                case LIGHTNING: this.color = Color.YELLOW; break;
                default: this.color = Color.DARKRED; break;
            }
        }

        public void update(double deltaTime) {
            if (!active) return;

            timeAlive += deltaTime;

            // Détruire le projectile s'il est trop vieux
            if (timeAlive > MAX_LIFETIME) {
                active = false;
                return;
            }

            position = position.add(velocity.multiply(deltaTime));

            // Vérifier si le projectile a atteint sa cible (distance approximative)
            double distanceToTarget = position.distance(target);
            if (distanceToTarget < 0.5) {
                active = false;
            }
        }
    }

    public abstract static class Entity {
        public Point2D position;
        public Stats stats;
        public EnemyClass entityClass;
        public double lastAttackTime;
        public double lastMoveTime;
        public boolean isPlayer;

        public Entity(Point2D position, Stats stats, EnemyClass entityClass, boolean isPlayer) {
            this.position = position;
            this.stats = stats;
            this.entityClass = entityClass;
            this.isPlayer = isPlayer;
            this.lastAttackTime = 0;
            this.lastMoveTime = 0;
        }

        public abstract void update(GameModel model, List<Entity> allEntities, List<Projectile> projectiles, double currentTime);

        public boolean canAttack(double currentTime) {
            return currentTime - lastAttackTime >= (1.0 / stats.attackSpeed);
        }

        public boolean canMove(double currentTime) {
            return currentTime - lastMoveTime >= (1.0 / stats.moveSpeed);
        }

        public int takeDamage(int damage, DamageType damageType) {
            double resistance = stats.resistances.get(damageType);
            int finalDamage = (int) (damage * (1.0 - resistance));
            stats.health = Math.max(0, stats.health - finalDamage);
            return finalDamage;
        }

        public double distanceTo(Entity other) {
            return position.distance(other.position);
        }

        public boolean isInRange(Entity target) {
            return distanceTo(target) <= stats.range;
        }
    }

    public static class Player extends Entity {
        public Player(Point2D position) {
            super(position, createPlayerStats(), EnemyClass.WARRIOR, true);
        }

        private static Stats createPlayerStats() {
            Stats stats = new Stats(100, 25, 1.0, 1.5, 1.5);
            // Le joueur peut avoir des résistances via équipement
            stats.setResistance(DamageType.PHYSICAL, 0.1); // 10% résistance physique de base
            return stats;
        }

        @Override
        public void update(GameModel model, List<Entity> allEntities, List<Projectile> projectiles, double currentTime) {
            // Le joueur est contrôlé par l'utilisateur
            this.position = model.getCurrentInterpolatedPosition();
        }
    }

    public static class Enemy extends Entity {
        public BehaviorType behavior;
        public Point2D homePosition;
        public Entity target;
        public List<Point2D> path;
        public int pathIndex;
        public double aggroRange;
        public double lastPathfindTime;
        public String packId; // Pour identifier les meutes
        public double lastDamageTime; // Pour éviter le spam de dégâts
        public double stateChangeTime; // Pour variations de comportement

        public Enemy(Point2D position, EnemyClass enemyClass, BehaviorType behavior) {
            super(position, createEnemyStats(enemyClass), enemyClass, false);
            this.behavior = behavior;
            this.homePosition = position;
            this.aggroRange = calculateAggroRange(enemyClass);
            this.path = new ArrayList<>();
            this.pathIndex = 0;
            this.lastPathfindTime = 0;
            this.lastDamageTime = 0;
            this.stateChangeTime = 0;
            this.packId = behavior == BehaviorType.PACK ? "pack_" + System.currentTimeMillis() : null;
        }

        private static Stats createEnemyStats(EnemyClass enemyClass) {
            switch (enemyClass) {
                case WARRIOR:
                    Stats warrior = new Stats(60, 20, 0.8, 1.0, 1.2);
                    warrior.setResistance(DamageType.PHYSICAL, 0.2);
                    return warrior;

                case MAGE:
                    Stats mage = new Stats(40, 35, 0.6, 0.7, 4.0);
                    mage.setResistance(DamageType.FIRE, 0.5);
                    mage.setResistance(DamageType.ICE, 0.3);
                    return mage;

                case ARCHER:
                    Stats archer = new Stats(50, 25, 1.0, 1.2, 5.0);
                    archer.setResistance(DamageType.PHYSICAL, 0.1);
                    return archer;

                case ELITE_WARRIOR:
                    Stats eliteWarrior = new Stats(120, 35, 1.0, 1.2, 1.5);
                    eliteWarrior.setResistance(DamageType.PHYSICAL, 0.4);
                    eliteWarrior.setResistance(DamageType.FIRE, 0.2);
                    return eliteWarrior;

                case ELITE_MAGE:
                    Stats eliteMage = new Stats(80, 50, 0.8, 1.0, 6.0);
                    eliteMage.setResistance(DamageType.FIRE, 0.7);
                    eliteMage.setResistance(DamageType.ICE, 0.7);
                    eliteMage.setResistance(DamageType.LIGHTNING, 0.5);
                    return eliteMage;

                case ELITE_ARCHER:
                    Stats eliteArcher = new Stats(90, 40, 1.2, 1.5, 7.0);
                    eliteArcher.setResistance(DamageType.PHYSICAL, 0.3);
                    eliteArcher.setResistance(DamageType.POISON, 0.4);
                    return eliteArcher;

                case BOSS:
                    Stats boss = new Stats(300, 60, 0.7, 0.8, 3.0);
                    boss.setResistance(DamageType.PHYSICAL, 0.3);
                    boss.setResistance(DamageType.FIRE, 0.4);
                    boss.setResistance(DamageType.ICE, 0.4);
                    boss.setResistance(DamageType.POISON, 0.6);
                    return boss;

                default:
                    return new Stats(50, 20, 1.0, 1.0, 1.5);
            }
        }

        private double calculateAggroRange(EnemyClass enemyClass) {
            switch (enemyClass) {
                case MAGE:
                case ELITE_MAGE:
                    return 6.0;
                case ARCHER:
                case ELITE_ARCHER:
                    return 8.0;
                case BOSS:
                    return 10.0;
                default:
                    return 4.0;
            }
        }

        @Override
        public void update(GameModel model, List<Entity> allEntities, List<Projectile> projectiles, double currentTime) {
            // Trouver le joueur
            Player player = findPlayer(allEntities);
            if (player == null || !player.stats.isAlive()) {
                target = null;
                return;
            }

            double distanceToPlayer = distanceTo(player);

            // Gestion de l'aggro
            if (target == null && distanceToPlayer <= aggroRange) {
                target = player;
                stateChangeTime = currentTime;
                if (behavior == BehaviorType.PACK) {
                    alertPackMembers(allEntities, player);
                }
            }

            // Perdre l'aggro si trop loin (pour les gardiens)
            if (target != null && behavior == BehaviorType.GUARDIAN) {
                double distanceFromHome = position.distance(homePosition);
                if (distanceFromHome > aggroRange * 2) {
                    target = null;
                    startPathfinding(model, homePosition, currentTime);
                }
            }

            // Comportement selon l'état
            if (target != null) {
                handleCombatBehavior(model, projectiles, currentTime);
            } else {
                handleIdleBehavior(model, currentTime);
            }

            // Mise à jour du pathfinding
            followPath(currentTime);
        }

        private void handleCombatBehavior(GameModel model, List<Projectile> projectiles, double currentTime) {
            if (isInRange(target)) {
                // Attaquer
                if (canAttack(currentTime)) {
                    attack(target, projectiles, currentTime);
                }

                // Comportement spécial selon la classe
                handleSpecialBehavior(model, currentTime);
            } else {
                // Se rapprocher
                if (canMove(currentTime) && (currentTime - lastPathfindTime > 0.5)) {
                    startPathfinding(model, target.position, currentTime);
                }
            }
        }

        private void handleIdleBehavior(GameModel model, double currentTime) {
            switch (behavior) {
                case GUARDIAN:
                    // Retourner à la position de garde si trop loin
                    double distanceFromHome = position.distance(homePosition);
                    if (distanceFromHome > 2.0) {
                        if (currentTime - lastPathfindTime > 1.0) {
                            startPathfinding(model, homePosition, currentTime);
                        }
                    }
                    break;

                case SOLITARY:
                    // Patrouille aléatoire
                    if (currentTime - stateChangeTime > 5.0 && Math.random() < 0.3) {
                        Point2D randomTarget = generateRandomPatrolPoint();
                        if (randomTarget != null) {
                            startPathfinding(model, randomTarget, currentTime);
                            stateChangeTime = currentTime;
                        }
                    }
                    break;

                case PACK:
                    // Rester près des autres membres de la meute
                    maintainPackCohesion(model, currentTime);
                    break;
            }
        }

        private void handleSpecialBehavior(GameModel model, double currentTime) {
            switch (entityClass) {
                case MAGE:
                case ELITE_MAGE:
                    // Les mages essaient de garder leurs distances
                    if (distanceTo(target) < stats.range * 0.7) {
                        retreatFromTarget(model, currentTime);
                    }
                    break;

                case BOSS:
                    // Le boss a des capacités spéciales
                    if (stats.getHealthPercent() < 0.5 && currentTime - lastDamageTime > 3.0) {
                        performBossSpecialAttack(model, currentTime);
                    }
                    break;
            }
        }

        private void retreatFromTarget(GameModel model, double currentTime) {
            if (target == null) return;

            // Calculer une position de retraite
            double dx = position.getX() - target.position.getX();
            double dy = position.getY() - target.position.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance > 0) {
                double retreatDistance = 2.0;
                Point2D retreatPos = new Point2D(
                        position.getX() + (dx / distance) * retreatDistance,
                        position.getY() + (dy / distance) * retreatDistance
                );

                if (model.isValidTile((int)retreatPos.getX(), (int)retreatPos.getY()) &&
                        model.canWalkThrough((int)retreatPos.getX(), (int)retreatPos.getY())) {
                    startPathfinding(model, retreatPos, currentTime);
                }
            }
        }

        private void performBossSpecialAttack(GameModel model, double currentTime) {
            // Attaque spéciale du boss : tempête de projectiles
            System.out.println("👹 Le boss utilise une attaque spéciale!");

            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Point2D projectileTarget = new Point2D(
                        position.getX() + Math.cos(angle) * 5,
                        position.getY() + Math.sin(angle) * 5
                );

                // Créer un projectile de foudre
                // Note: Cette méthode sera appelée avec la liste des projectiles dans le contexte approprié
            }

            lastDamageTime = currentTime;
        }

        private void alertPackMembers(List<Entity> allEntities, Entity threat) {
            if (packId == null) return;

            for (Entity entity : allEntities) {
                if (entity instanceof Enemy) {
                    Enemy enemy = (Enemy) entity;
                    if (packId.equals(enemy.packId) && enemy.target == null) {
                        enemy.target = threat;
                        enemy.stateChangeTime = System.currentTimeMillis() / 1000.0;
                    }
                }
            }
        }

        private void maintainPackCohesion(GameModel model, double currentTime) {
            if (packId == null) return;

            // Trouver le centre de la meute
            Point2D packCenter = calculatePackCenter(model);
            if (packCenter != null) {
                double distanceToCenter = position.distance(packCenter);
                if (distanceToCenter > 5.0 && currentTime - lastPathfindTime > 2.0) {
                    startPathfinding(model, packCenter, currentTime);
                }
            }
        }

        private Point2D calculatePackCenter(GameModel model) {
            List<Enemy> packMembers = new ArrayList<>();

            // Trouver tous les membres de la meute
            for (Entity entity : model.getCombatSystem().getEntities()) {
                if (entity instanceof Enemy) {
                    Enemy enemy = (Enemy) entity;
                    if (packId.equals(enemy.packId) && enemy.stats.isAlive()) {
                        packMembers.add(enemy);
                    }
                }
            }

            if (packMembers.isEmpty()) return null;

            double totalX = 0, totalY = 0;
            for (Enemy member : packMembers) {
                totalX += member.position.getX();
                totalY += member.position.getY();
            }

            return new Point2D(totalX / packMembers.size(), totalY / packMembers.size());
        }

        private Point2D generateRandomPatrolPoint() {
            Random rand = new Random();
            double angle = rand.nextDouble() * Math.PI * 2;
            double distance = 2 + rand.nextDouble() * 3; // 2-5 cases

            return new Point2D(
                    homePosition.getX() + Math.cos(angle) * distance,
                    homePosition.getY() + Math.sin(angle) * distance
            );
        }

        private Player findPlayer(List<Entity> entities) {
            for (Entity entity : entities) {
                if (entity instanceof Player) {
                    return (Player) entity;
                }
            }
            return null;
        }

        private void attack(Entity target, List<Projectile> projectiles, double currentTime) {
            lastAttackTime = currentTime;

            DamageType damageType = getDamageType();
            int damage = stats.damage + (int)(Math.random() * 10 - 5); // Variation ±5

            if (isRangedClass()) {
                // Créer un projectile
                Projectile projectile = new Projectile(position, target.position, damageType, damage, 8.0, this);
                projectiles.add(projectile);
            } else {
                // Attaque de mêlée directe
                if (currentTime - lastDamageTime > 0.5) { // Éviter le spam de dégâts
                    int finalDamage = target.takeDamage(damage, damageType);
                    lastDamageTime = currentTime;
                }
            }
        }

        private DamageType getDamageType() {
            switch (entityClass) {
                case MAGE:
                case ELITE_MAGE:
                    return Math.random() < 0.5 ? DamageType.FIRE : DamageType.ICE;
                case ELITE_ARCHER:
                    return Math.random() < 0.3 ? DamageType.POISON : DamageType.PHYSICAL;
                case BOSS:
                    DamageType[] types = {DamageType.FIRE, DamageType.LIGHTNING, DamageType.POISON};
                    return types[(int)(Math.random() * types.length)];
                default:
                    return DamageType.PHYSICAL;
            }
        }

        private boolean isRangedClass() {
            return entityClass == EnemyClass.MAGE || entityClass == EnemyClass.ARCHER ||
                    entityClass == EnemyClass.ELITE_MAGE || entityClass == EnemyClass.ELITE_ARCHER;
        }

        private void startPathfinding(GameModel model, Point2D destination, double currentTime) {
            lastPathfindTime = currentTime;

            // Pathfinding simple - peut être amélioré avec A*
            path.clear();

            // Vérifier que la destination est valide
            if (model.isValidTile((int)destination.getX(), (int)destination.getY()) &&
                    model.canWalkThrough((int)destination.getX(), (int)destination.getY())) {
                path.add(destination);
                pathIndex = 0;
            }
        }

        private void followPath(double currentTime) {
            if (path.isEmpty() || pathIndex >= path.size()) return;

            Point2D nextPoint = path.get(pathIndex);
            double distance = position.distance(nextPoint);

            if (distance < 0.5) {
                pathIndex++;
                lastMoveTime = currentTime;
                return;
            }

            if (canMove(currentTime)) {
                // Se déplacer vers le point suivant
                double dx = nextPoint.getX() - position.getX();
                double dy = nextPoint.getY() - position.getY();
                double length = Math.sqrt(dx * dx + dy * dy);

                if (length > 0) {
                    double moveDistance = stats.moveSpeed * 0.5; // Ajusté pour le deltaTime
                    double newX = position.getX() + (dx / length) * moveDistance;
                    double newY = position.getY() + (dy / length) * moveDistance;
                    position = new Point2D(newX, newY);
                }

                lastMoveTime = currentTime;
            }
        }
    }

    // ================================
    // GESTION PRINCIPALE DU SYSTÈME
    // ================================

    private List<Entity> entities = new ArrayList<>();
    private List<Projectile> projectiles = new ArrayList<>();
    private double gameTime = 0;
    private Random random = new Random();

    public void addEntity(Entity entity) {
        entities.add(entity);
        System.out.println("🎮 Entité ajoutée: " + (entity.isPlayer ? "Joueur" : entity.entityClass));
    }

    public void removeEntity(Entity entity) {
        entities.remove(entity);
    }

    public List<Entity> getEntities() {
        return new ArrayList<>(entities);
    }

    public List<Projectile> getProjectiles() {
        return new ArrayList<>(projectiles);
    }

    public void update(GameModel model, double deltaTime) {
        gameTime += deltaTime;

        // Mettre à jour toutes les entités
        updateEntities(model, deltaTime);

        // Mettre à jour les projectiles
        updateProjectiles(deltaTime);

        // Nettoyer les entités mortes
        cleanupDeadEntities();
    }

    private void updateEntities(GameModel model, double deltaTime) {
        Iterator<Entity> entityIterator = entities.iterator();
        while (entityIterator.hasNext()) {
            Entity entity = entityIterator.next();

            if (!entity.stats.isAlive()) {
                if (!entity.isPlayer) {
                    // L'entité sera supprimée dans cleanupDeadEntities()
                    continue;
                } else {
                    // Le joueur est mort - le GameModel gère cela
                    continue;
                }
            }

            entity.update(model, entities, projectiles, gameTime);
        }
    }

    private void updateProjectiles(double deltaTime) {
        Iterator<Projectile> projectileIterator = projectiles.iterator();
        while (projectileIterator.hasNext()) {
            Projectile projectile = projectileIterator.next();

            if (!projectile.active) {
                projectileIterator.remove();
                continue;
            }

            projectile.update(deltaTime);

            // Vérifier les collisions avec les entités
            if (checkProjectileCollisions(projectile)) {
                projectileIterator.remove();
            }
        }
    }

    private void cleanupDeadEntities() {
        Iterator<Entity> entityIterator = entities.iterator();
        while (entityIterator.hasNext()) {
            Entity entity = entityIterator.next();

            if (!entity.isPlayer && !entity.stats.isAlive()) {
                entityIterator.remove();
            }
        }
    }

    private boolean checkProjectileCollisions(Projectile projectile) {
        for (Entity entity : entities) {
            if (entity == projectile.source) continue; // Pas de friendly fire pour l'instant

            double distance = projectile.position.distance(entity.position);
            if (distance < 0.5) {
                // Collision!
                int finalDamage = entity.takeDamage(projectile.damage, projectile.damageType);
                projectile.active = false;
                return true;
            }
        }
        return false;
    }

    // ================================
    // MÉTHODES DE SPAWN D'ENNEMIS
    // ================================

    public void spawnEnemyPack(GameModel model, Point2D centerPosition, EnemyClass baseClass, int count) {
        String packId = "pack_" + System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i) / count;
            double radius = 2.0;
            Point2D spawnPos = new Point2D(
                    centerPosition.getX() + Math.cos(angle) * radius,
                    centerPosition.getY() + Math.sin(angle) * radius
            );

            Enemy enemy = new Enemy(spawnPos, baseClass, BehaviorType.PACK);
            enemy.packId = packId;
            addEntity(enemy);
        }

        System.out.println("🐺 Meute de " + count + " " + baseClass + " apparue!");
    }

    public Entity spawnSolitaryEnemy(Point2D position, EnemyClass enemyClass) {
        Enemy enemy = new Enemy(position, enemyClass, BehaviorType.SOLITARY);
        addEntity(enemy);
        return enemy;
    }

    public Entity spawnGuardian(Point2D position, EnemyClass enemyClass) {
        Enemy enemy = new Enemy(position, enemyClass, BehaviorType.GUARDIAN);
        addEntity(enemy);
        return enemy;
    }

    // ================================
    // MÉTHODES UTILITAIRES
    // ================================

    public int getAliveEnemyCount() {
        return (int) entities.stream()
                .filter(e -> !e.isPlayer && e.stats.isAlive())
                .count();
    }

    public int getTotalEnemyCount() {
        return (int) entities.stream()
                .filter(e -> !e.isPlayer)
                .count();
    }

    public boolean hasBoss() {
        return entities.stream()
                .anyMatch(e -> !e.isPlayer &&
                        e.entityClass == EnemyClass.BOSS &&
                        e.stats.isAlive());
    }

    public Entity findNearestEnemyToPosition(Point2D position, double maxDistance) {
        Entity nearest = null;
        double minDistance = maxDistance;

        for (Entity entity : entities) {
            if (entity.isPlayer || !entity.stats.isAlive()) continue;

            double distance = entity.position.distance(position);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = entity;
            }
        }

        return nearest;
    }

    public List<Entity> getEnemiesInRadius(Point2D center, double radius) {
        List<Entity> enemiesInRadius = new ArrayList<>();

        for (Entity entity : entities) {
            if (entity.isPlayer || !entity.stats.isAlive()) continue;

            if (entity.position.distance(center) <= radius) {
                enemiesInRadius.add(entity);
            }
        }

        return enemiesInRadius;
    }

    public void printStats() {
        System.out.println("=== Statistiques du Système de Combat ===");
        System.out.println("Entités totales: " + entities.size());
        System.out.println("Ennemis vivants: " + getAliveEnemyCount());
        System.out.println("Projectiles actifs: " + projectiles.size());
        System.out.println("Boss présent: " + (hasBoss() ? "OUI" : "NON"));
        System.out.println("Temps de jeu: " + String.format("%.1f", gameTime) + "s");

        // Statistiques par classe
        Map<EnemyClass, Integer> classCounts = new HashMap<>();
        for (Entity entity : entities) {
            if (!entity.isPlayer && entity.stats.isAlive()) {
                classCounts.put(entity.entityClass,
                        classCounts.getOrDefault(entity.entityClass, 0) + 1);
            }
        }

        System.out.println("Répartition par classe:");
        for (Map.Entry<EnemyClass, Integer> entry : classCounts.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
        System.out.println("=====================================");
    }
}