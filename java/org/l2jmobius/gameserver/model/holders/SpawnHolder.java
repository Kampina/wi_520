package org.l2jmobius.gameserver.model.holders;

public class SpawnHolder
{
private final int _id;
private final int _x;
private final int _y;
private final int _z;
private final int _heading;

public SpawnHolder(int id, int x, int y, int z, int heading)
{
_id = id;
_x = x;
_y = y;
_z = z;
_heading = heading;
}

public int getId()
{
return _id;
}

public int getX()
{
return _x;
}

public int getY()
{
return _y;
}

public int getZ()
{
return _z;
}

public int getHeading()
{
return _heading;
}
}
