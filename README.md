RotorControl
============

This is an Android antenna rotor (rotator) GUI to go with hamlib for amateur radio operators.

It uses hamlib (hamlib.sourceforge.net).

You need to change the IP address RotorControl.java, and rebuild (using the Android ADT).
Then using your rotor hooked up to hamlib, you start the rotctld with something like this:

$ rotctld -m 801 -Cmax_az=160,min_az=-160 -L -r /dev/ttyS0

Read the man pages on rotctld.
