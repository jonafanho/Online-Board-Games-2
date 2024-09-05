# Online Board Games

Play board games online with others! Made with Spring and Angular.

This README is still work in progress. Come back soon for updates!

## Development

### Debugging

1. Start the local Angular server
   ```npm
   npm run start
   ```
2. Start Spring by running `src/main/java/org/game/Main.java`

### Building

1. Generate required classes
   ```gradle
   gradle generateSchemaClasses
   ```
2. Build the Angular project
   ```npm
   npm run build
   ```
3. Build the JAR file
   ```gradle
   gradle build
   ```

### Adding a Game

1. Create a JSON file in `buildSrc/src/main/resources/schema/library`
2. Generate required classes
   ```gradle
   gradle generateSchemaClasses
   ```
3. Create the file `src/main/java/org/game/library/<game-id>/GameState.java` and add the game logic
