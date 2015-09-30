# brockton-bay

Very early prototype of a superhero management game in Clojure.



@World
*Map (by ID) of Person
*List of Location

@Person
*ID
*Name/Codename
*Stats:
*** Speed
*** Damage
*** Armour
*** HP
*Traits/Abilities (someday)
*Faction
*Location
*Activity (someday)

@Multiplayer
- #Ask nb human players, #Ask nb AI players
- For each player, pick a random Faction name, then generate a roster of People from a list of predefined ones.
- Each turn:
(Q: maybe 3 turns?)
-- For each Location, set a random Payoff. (Amounts increase in later turns.)
-- Print full state of the World.
-- Localize the idle Person with the lowest Speed.
-- #Ask its player in which Location they should go (or if they should stay home).
-- Move them to that Location.
-- If there are People from different Factions and no agreement yet, then for each such pair of Factions:
(Q: should this be done here, or after every Person is committed?)
--- Find the highest-Speed Person of each of the two Factions.
--- #Ask the lowest-Speed one's player if they want to SHARE, FIGHT, or FLEE for this particular Location and Faction.
---- If SHARE, #ask the other player if they SHARE, FIGHT, or FLEE.
---- If FIGHT, #ask the other player if they FIGHT or FLEE.
---- If FLEE, no need to ask the other player (auto FIGHT).
-- Print full state of the world again, #ask the next low-Speed idle Person etc. until all have been dispatched.
-- For each location in ascending Payoff order:
--- Do a FLEE combat round: 
---- While there are FLEE People at the location:
---- Activate each Person in descending Speed order.
---- FLEEing People: they take [some] money off the Payoff (given to their player) and leave the location.
---- Non-FLEEing People: #Ask their player for a FLEEing Person to attack (can't choose "none"). 
(Q: should they just attack a random one, to save time?)
---- Resolve that attack. 
---- Check for <= 0 HP and remove them from the World.
--- Do FIGHT combat rounds while there are People from Factions in a FIGHT/FIGHT or SHARE/FIGHT agreement:
---- While there are People from Factions in a FIGHT/FIGHT or SHARE/FIGHT agreement:
---- Activate each Person in descending Speed order.
---- #Ask their player for a Person from an opposing Faction to attack (can't choose "none"). 
(Q: should they just attack a random one, to save time?)
---- Resolve that attack. Attack bonus for the FIGHT side of a SHARE/FIGHT agreement. 
---- Check for <= 0 HP and remove them from the World.
--- Do a SHARE split:
---- Each player in SHARE/SHARE gets one share of the Payoff for each surviving Person they have at the Location.
(Q: should there be some amount of healing between turns?)
- After the final turn, print the final state of the World, with emphasis on the total amount of money each player has.
- Richest person wins.


@Current task list:
V Bugfixes
V-- Multiple agreements
V-- Missing location names in Agreement ask

- Make SHARE/FIGHT/FLEE actually do something
V-- FLEE round
--- SHARE people aren't enemies
--- FIGHT/SHARE combat bonus
V-- SHARE round

- Make speed matter
--- In the FLEE round
--- In the FIGHT round

- Pretty up the GUI a little
V-- Show People details
V-- Show location names
--- Show agreements
--- Scrollable
--- Get the Ask windows out of the way
--- Some kind of log, especially for combat
--- More sorting (in the big People list, in final score)
--- Move People to the Location area when they're placed their.

V Make the AI be an AI
V-- Choose location to place character.
V-- Choose SHARE/FIGHT/FLEE

{OLD}
@Plays-itself main game loop
- For each character:
-- Build list of enemies (characters from other factions) in the same location.
-- If non-empty:
--- Pick which character to attack.
---- Damage > Resistance
---- Tiebreak: highest Resistance
---- Tiebreak 2: highest Damage
---- Tiebreak 3: lowest Toughness
---- Tiebreak 4: random
--- Toughness -= (Damage - Resistance)
--- Check for death.
-- If empty:
--- Move to the location of a random enemy.
--- If no enemy: game over, surviving faction (if any) wins.