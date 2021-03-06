package org.usfirst.frc.team4256.robot.Autonomous;

import com.cyborgcats.reusable.Autonomous.Leash;
import com.cyborgcats.reusable.Autonomous.Odometer;
import com.cyborgcats.reusable.Autonomous.P_Curve;
import com.cyborgcats.reusable.Autonomous.P_Curve.Function;
import com.cyborgcats.reusable.Autonomous.Path;

public final class S_Slither extends Strategy2017 {
	
	public S_Slither(final StartingPosition posI, final char[] gameData, final Odometer odometer) {super(posI, odometer);}
	public S_Slither(final Odometer odometer) {super(StartingPosition.CENTER, odometer);}
	
	@Override
	protected Leash getLeash() {
		final Function x = (t) -> 2.0*Math.sin(t);
		final Function y = (t) -> t;
		
		final Path a = new P_Curve(x, y, 0.0, 2.0*Math.PI);
		final Path[] path = new Path[] {a};
		return new Leash(path, /*leash length*/1.5, /*growth rate*/0.1);
	}
}
