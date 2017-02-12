package org.usfirst.frc.team4256.robot;

import com.cyborgcats.reusable.R_Gyro;
import com.cyborgcats.reusable.V_Compass;

public class R_DriveTrain {
	private static final double Modules_side = 21.85;//MOD refers to the point at which the wheel touches the ground
	private static final double Modules_front = 25.85;
	private R_Gyro gyro;
	private R_SwerveModule module1;//arranged clockwise
	private R_SwerveModule module2;
	private R_SwerveModule module3;
	private R_SwerveModule module4;
	
	public R_DriveTrain(final R_Gyro gyro, final R_SwerveModule module1, final R_SwerveModule module2, final R_SwerveModule module3, final R_SwerveModule module4) {
		this.gyro = gyro;
		this.module1 = module1;
		this.module2 = module2;
		this.module3 = module3;
		this.module4 = module4;
	}
	//private static final double RADIUS = Math.sqrt(Math.pow(MOD2MOD_SIDE, 2) + Math.pow(MOD2MOD_FRONT, 2))/2;
	private static final double Module1_frontAngle = Math.toDegrees(Math.atan(Modules_front/Modules_side));
	private static final double Module2_frontAngle = 180 - Module1_frontAngle;
	private static final double Module3_frontAngle = -Module2_frontAngle;
	private static final double Module4_frontAngle = -Module1_frontAngle;
	
	public void control(final double direction, final double speed, final double spin) {
		module1.rotateTo(direction, gyro.getCurrentAngle());
		module2.rotateTo(direction, gyro.getCurrentAngle());
		module3.rotateTo(direction, gyro.getCurrentAngle());
		module4.rotateTo(direction, gyro.getCurrentAngle());
		if (isThere(4)) {
			double module1_fieldAngle = V_Compass.validateAngle(gyro.getCurrentAngle() + Module1_frontAngle);
			double module2_fieldAngle = V_Compass.validateAngle(gyro.getCurrentAngle() + Module2_frontAngle);
			double module3_fieldAngle = V_Compass.validateAngle(gyro.getCurrentAngle() + Module3_frontAngle);
			double module4_fieldAngle = V_Compass.validateAngle(gyro.getCurrentAngle() + Module4_frontAngle);
			double speed1 = speed*spin*Math.cos(Math.toRadians(module1_fieldAngle - 90));
			double speed2 = speed*spin*Math.cos(Math.toRadians(module2_fieldAngle - 90));
			double speed3 = speed*spin*Math.cos(Math.toRadians(module3_fieldAngle - 90));
			double speed4 = speed*spin*Math.cos(Math.toRadians(module4_fieldAngle - 90));
			double x = 4*speed/(speed1 + speed2 + speed3 + speed4);
			speed1 *= x;
			speed2 *= x;
			speed3 *= x;
			speed4 *= x;
			module1.set(speed1);
			module2.set(speed2);
			module3.set(speed3);
			module4.set(speed4);
		}
	}
	
	public boolean isThere(final double threshold) {
		return module1.isThere(threshold) && module2.isThere(threshold) && module3.isThere(threshold) && module4.isThere(threshold);
	}
}