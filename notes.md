ensure debug is feature complete. Require creative mode. 
Only one debug, client side. Right way? Teleport client to server via packet?
Folder organization? Client/server? Some way to cleanly separate/indicate what is client only, what is server. 
Proper implementation of all features 

Code structure: use my logger format. Move numerics to leading variables. Local use should stay local defined. What things go in config? 

Add alternate placements with probs to json. *not* matter for picker. 

Can we remove (no doubt cover up) the cyclops eye? So shearing leaves it blind

Butt eyes: horse, pig, pillager beast, bees (one)

Deal with babies. 
Eyes on armor stands and boats? Realistic boat mod? Minecarts?
Make eyes emit light. 
Eyes on players 
Eyes on armor (helmet, shield, leggings). 
Server does not need to update iris dynamics, only needs to know “start wink” etc. server maintains eye existence and state but not physics. Don’t need different players to agree on precise iris location or exactly when a wink starts and stops. Only color, etc. 

GooglyLib to expose setting eye state for other modders? Separate mod or built in?
Switch to actual cylinder not rotated quads
Use blockbench to put eye locators. Make json compatible so can paste
Will it work with realistic bees?
Can we tell if a villager is running in fear? If so, scared eye effect. 
Effects: stare, blink, scared, colored, swirly, black and blue if injured, light emitting (squid), droopy, side eye, eye roll, cross eye
Make blink have eyelid, colorable
Maven upload and obfuscate 
Dambala to design docs website 
Contact ichun
Fabric port
1.21 port
Do it the Forge way
Assess sidedness
Eye-bearing heads + Heads mod (§2)	Hard	Separate render path (head block/item), custom NBT, soft-dep on another mod.
Armor slot eyes (§4)	Hard / orthogonal	Custom slot or repurposed equipment + GUI; cuts against the data-driven design.
Eyebrows (§4)	Hard (art + state)	New model geometry beyond cornea/iris and an expression state machine. Art-heavy.
eyes in item frame
centralize hard coded values as constants in the class (or wherever makes sense).
truly track each eye independently per mob.
