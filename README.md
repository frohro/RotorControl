RotorControl
============

This is an Android antenna rotor (rotator) GUI to go with hamlib for amateur radio operators.

It uses hamlib (hamlib.sourceforge.net).  You need to set the IP address and port in the app
to match the server that is connected to the rotator controller via a serial port.

The command for my Heath Intellirotor on the server is this:
$ rotctld -m 801 -Cmax_az=160,min_az=-160 -L -r /dev/ttyS0

Read the man pages on rotctld, and the help on the app.
