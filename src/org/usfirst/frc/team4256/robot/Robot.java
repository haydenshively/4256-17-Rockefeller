//DRIVER
//start + back: align
//left stick, both axis: raw speed and direction
//right stick, x axis: raw spin
//right stick, press: snail mode
//LB: boolean climber
//LT: toggle clamp
//RB: turbo mode (drive and climber)
//RT: toggle lift
//X: left gear orientation
//A: center gear orientation
//B: right gear orientation
//Y: loading station orientation
//dpad down: toggle gearer

//GUNNER
//start + back: gyro reset
//left stick, y axis: delta gimbal y
//right stick, x axis: delta gimbal x
//LT: reverse driver's climbing commands

package org.usfirst.frc.team4256.robot;

import java.util.HashMap;
import java.util.Map;

import com.cyborgcats.reusable.R_CANTalon;
import com.cyborgcats.reusable.R_Gyro;
import com.cyborgcats.reusable.R_Xbox;
import com.cyborgcats.reusable.V_Fridge;
import com.cyborgcats.reusable.V_PID;

import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.GenericHID.RumbleType;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.RobotState;
import edu.wpi.first.wpilibj.networktables.NetworkTable;

public class Robot extends IterativeRobot {
	//{Human Input}
	private static final R_Xbox driver = new R_Xbox(0);
	private static final R_Xbox gunner = new R_Xbox(1);
	private static final Map<Integer, Double> buttons2angle = new HashMap<Integer, Double>();
	private static final int[] gearButtons = new int[] {R_Xbox.BUTTON_X, R_Xbox.BUTTON_A, R_Xbox.BUTTON_B, R_Xbox.BUTTON_Y};
	private static Long handsOffTime = System.currentTimeMillis();
	private static double lockedAngle = 0;
	//{Robot Input}
	private static final R_Gyro gyro = new R_Gyro(Parameters.Gyrometer_updateHz, 0, 0);
	private static NetworkTable rockefeller;
	private static NetworkTable edison;
	private static NetworkTable tesla;
	private static double metersX = 0;
	private static double metersY = 0;
	
	private static boolean lifterRaised = false;
	private static int autoMode = 1;
	private static int autoStep = 0;
	//{Robot Output}
	private static final Compressor compressor = new Compressor(0);
	
	private static final R_SwerveModule moduleA = new R_SwerveModule(Parameters.Swerve_rotatorA, true, Parameters.Swerve_driveAA, Parameters.Swerve_driveAB, Parameters.Swerve_calibratorA);
	private static final R_SwerveModule moduleB = new R_SwerveModule(Parameters.Swerve_rotatorB, true, Parameters.Swerve_driveBA, Parameters.Swerve_driveBB, Parameters.Swerve_calibratorB);
	private static final R_SwerveModule moduleC = new R_SwerveModule(Parameters.Swerve_rotatorC, true, Parameters.Swerve_driveCA, Parameters.Swerve_driveCB, Parameters.Swerve_calibratorC);
	private static final R_SwerveModule moduleD = new R_SwerveModule(Parameters.Swerve_rotatorD, true, Parameters.Swerve_driveDA, Parameters.Swerve_driveDB, Parameters.Swerve_calibratorD);
	private static final R_DriveTrain swerve = new R_DriveTrain(gyro, moduleA, moduleB, moduleC, moduleD);
	
	private static final R_CANTalon climber = new R_CANTalon(Parameters.Climber, 17, R_CANTalon.voltage);
	
	private static final R_CANTalon lift = new R_CANTalon(Parameters.Lift, 1, R_CANTalon.voltage);
	private static final DoubleSolenoid clamp = new DoubleSolenoid(Parameters.Clamp_module, Parameters.Clamp_forward, Parameters.Clamp_reverse);
	private static final DoubleSolenoid gearer = new DoubleSolenoid(Parameters.Gearer_module, Parameters.Gearer_forward, Parameters.Gearer_reverse);
	
	@Override
	public void robotInit() {
		//{Robot Input}
		rockefeller = NetworkTable.getTable("rockefeller");
		edison = NetworkTable.getTable("edison");
		tesla = NetworkTable.getTable("tesla");
		//{Robot Output}
		compressor.clearAllPCMStickyFaults();
		swerve.init();
		V_PID.set("forward", Parameters.forwardP, Parameters.forwardI, Parameters.forwardD);
		V_PID.set("strafe", Parameters.strafeP, Parameters.strafeI, Parameters.strafeD);
		V_PID.set("spin", Parameters.spinP, Parameters.spinI, Parameters.spinD);
		climber.init();
		climber.setVoltageCompensationRampRate(24);
		lift.init();
	}

	@Override
	public void autonomousInit() {
		gyro.reset();
		autoMode = (int)rockefeller.getNumber("auto mode", 1);
		autoStep = 0;
		V_PID.clear("forward");
		V_PID.clear("strafe");
		V_PID.clear("spin");
	}
	
	@Override
	public void teleopInit() {
		if (DriverStation.getInstance().getAlliance() != DriverStation.Alliance.Red) {//TODO override brake modes
			Parameters.loadingStation += 90;
		}
		buttons2angle.put(R_Xbox.BUTTON_X, Parameters.leftGear);
		buttons2angle.put(R_Xbox.BUTTON_A, Parameters.centerGear);
		buttons2angle.put(R_Xbox.BUTTON_B, Parameters.rightGear);
		buttons2angle.put(R_Xbox.BUTTON_Y, Parameters.loadingStation);
		V_PID.clear("spin");
		lockedAngle = gyro.getCurrentAngle();
	}
	
	@Override
	public void testInit() {//TODO these numbers should come from the ZED/TK1
		tesla.putNumber("x", 0);
		tesla.putNumber("y", 0);
		tesla.putNumber("expected x", 0);
		tesla.putNumber("expected y", 0);
		tesla.putNumber("expected angle", 0);
	}
	
	@Override
	public void disabledInit() {
	}
	
	@Override
	public void robotPeriodic() {
		rockefeller.putBoolean("old gear out", gearer.get().equals(DoubleSolenoid.Value.kForward));
		rockefeller.putBoolean("clamp open", clamp.get().equals(DoubleSolenoid.Value.kForward));
		rockefeller.putBoolean("lift down", lifterRaised);
		rockefeller.putBoolean("aligning", swerve.isAligning());
		rockefeller.putBoolean("aligned", swerve.isAligned());
	}
	
	@Override
	public void autonomousPeriodic() {
		if (RobotState.isAutonomous()) {
			if (!swerve.isAligned()) {//ALIGN
				swerve.align(.004);
				moduleA.setTareAngle(9);	moduleB.setTareAngle(-3);	moduleC.setTareAngle(6);	moduleD.setTareAngle(8);
			}
			if (!lifterRaised) {//RAISE LIFTER
				lift.set(-.16);
			}else {
				lift.set(-.08);
			}
			if (lift.getOutputCurrent() > 2) {lifterRaised = true;}
			
			switch (autoMode) {
			case 0://LEFT GEAR
				V_Instructions.follow(Parameters.leftInstructions, autoStep, swerve, gyro);
				if (V_Instructions.readyToMoveOn() && V_Instructions.canMoveOn()) {
					autoStep++;
				}else if (!V_Instructions.canMoveOn()) {
					clamp.set(DoubleSolenoid.Value.kForward);
					lift.set(0);
				}
				break;
			case 1://MIDDLE GEAR
				V_Instructions.follow(Parameters.middleInstructions, autoStep, swerve, gyro);
				if (V_Instructions.readyToMoveOn() && V_Instructions.canMoveOn()) {
					autoStep++;
				}else if (!V_Instructions.canMoveOn()) {
					double pegX = edison.getNumber("peg x", 0);
					if (edison.getNumber("targets", 0) > 0 && edison.getNumber("peg y", 0) < 210) {
						double xError = pegX - 170;//TODO what is actual center?
						double angleError = xError*45/100;//TODO tune
						swerve.holonomic(0 + angleError, 0.15, 0);//0 is there as a placeholder for values at other gears
					}else {
						//TODO need to move that last little bit still
						clamp.set(DoubleSolenoid.Value.kForward);
						lift.set(0);
					}
					//TODO could backup and cross line
				}
				break;
			case 2://RIGHT GEAR
				V_Instructions.follow(Parameters.rightInstructions, autoStep, swerve, gyro);
				if (V_Instructions.readyToMoveOn() && V_Instructions.canMoveOn()) {
					autoStep++;
				}else if (!V_Instructions.canMoveOn()) {
					clamp.set(DoubleSolenoid.Value.kForward);
					lift.set(0);
				}
				break;
			default:break;
			}
//			//{completing Talon updates}
//			moduleA.completeLoopUpdate();
//			moduleB.completeLoopUpdate();
//			moduleC.completeLoopUpdate();
//			moduleD.completeLoopUpdate();
//			climber.completeLoopUpdate();
//			lift.completeLoopUpdate();
		}
	}
	
	@Override
	public void teleopPeriodic() {
		if (driver.getRawButton(R_Xbox.BUTTON_START) && driver.getRawButton(R_Xbox.BUTTON_BACK)) {//SWERVE ALIGNMENT
			swerve.align(.004);//TODO limit how long this can take
			moduleA.setTareAngle(9);	moduleB.setTareAngle(-3);	moduleC.setTareAngle(6);	moduleD.setTareAngle(8);//TODO add to parameters
		}
		
		if (gunner.getRawButton(R_Xbox.BUTTON_START) && gunner.getRawButton(R_Xbox.BUTTON_BACK)) {//GYRO RESET
			gyro.reset();
			lockedAngle = gyro.getCurrentAngle();
			V_PID.clear("spin");
		}
		
		//{calculating speed}
		double speed = driver.getCurrentRadius(R_Xbox.STICK_LEFT, true);//--turbo mode
		if (!driver.getRawButton(R_Xbox.BUTTON_RB)) {speed *= .6;}//--normal mode
		speed *= speed;
		//{calculating raw spin}
		double spin = driver.getDeadbandedAxis(R_Xbox.AXIS_RIGHT_X);
		spin *= spin*Math.signum(spin)*.5;//--normal mode
		if (driver.getRawButton(R_Xbox.BUTTON_STICK_RIGHT)) {
			spin *= .5;//--snail mode
			if (speed == 0) {speed = .01;}//.01 restrains coast after spinning by hacking holonomic
		}
		//{adding driver aids}
		if (V_Fridge.becomesTrue("hands off", spin == 0)) {
			handsOffTime = System.currentTimeMillis();
			lockedAngle = gyro.getCurrentAngle();//remember angle when driver stops rotating
			V_PID.clear("spin");
		}if (spin == 0) {
			double spinError = 0;
			if (speed >= .3) {spinError = gyro.wornPath(lockedAngle);}//stop rotation drift at high speeds
			int gearButton = driver.mostRecentButton(gearButtons);
			if (driver.lastPress(gearButton) > handsOffTime) {spinError = gyro.wornPath(buttons2angle.get(gearButton));}
			if (Math.abs(spinError) > 3) {spin = V_PID.get("spin", spinError);}
		}
		
		swerve.holonomic(driver.getCurrentAngle(R_Xbox.STICK_LEFT, true), speed, spin);//SWERVE DRIVE
		
		if (driver.getRawButton(R_Xbox.BUTTON_LB)) {//CLIMBER
			double climbSpeed = driver.getRawButton(R_Xbox.BUTTON_RB) ? -1 : -.6;
			if (gunner.getAxisPress(R_Xbox.AXIS_LT, .5)) {climbSpeed *= -1;}
			climber.set(climbSpeed);
		}else {
			climber.set(0);
		}
		
		if (V_Fridge.freeze("POVSOUTH", driver.getPOV(0) == R_Xbox.POV_SOUTH)) {//GEARER
			gearer.set(DoubleSolenoid.Value.kForward);
		}else {
			gearer.set(DoubleSolenoid.Value.kReverse);
		}
		
		if (V_Fridge.freeze("AXISRT", driver.getAxisPress(R_Xbox.AXIS_RT, .5))) {//CLAMPER
			clamp.set(DoubleSolenoid.Value.kForward);
		}else {
			clamp.set(DoubleSolenoid.Value.kReverse);
		}
		
		if (V_Fridge.freeze("AXISLT", driver.getAxisPress(R_Xbox.AXIS_LT, .5))) {//LIFTER
			if (!lifterRaised) {
				lift.set(-.16);
			}else {
				lift.set(-.08);
			}
			if (lift.getOutputCurrent() > 2) {lifterRaised = true;}
		}else {
			if (lifterRaised) {
				lift.set(.13);
			}else {
				lift.set(0);
			}
			if (lift.getOutputCurrent() > 2) {lifterRaised = false;}
		}
		
		if (gyro.netAcceleration() >= 1) {
			driver.setRumble(RumbleType.kLeftRumble, 1);//DANGER RUMBLE
		}else {
			driver.setRumble(RumbleType.kLeftRumble, 0);
		}
		
		//{completing Talon updates}
		moduleA.completeLoopUpdate();
		moduleB.completeLoopUpdate();
		moduleC.completeLoopUpdate();
		moduleD.completeLoopUpdate();
		climber.completeLoopUpdate();
		lift.completeLoopUpdate();
	}
	
	@Override
	public void testPeriodic() {
		metersX = tesla.getNumber("x", metersX);
		metersY = tesla.getNumber("y", metersY);
		double expectedX = tesla.getNumber("expected x", metersX);
		double expectedY = tesla.getNumber("expected y", metersY);
		double expectedAngle = tesla.getNumber("expected angle", gyro.getCurrentAngle());
		double xError = expectedX - metersX;
		double yError = expectedY - metersY;
		double spinError = gyro.wornPath(expectedAngle);
		swerve.holonomic2(V_PID.get("forward", yError), V_PID.get("strafe", xError), V_PID.get("spin", spinError));
	}
	
	@Override
	public void disabledPeriodic() {
	}
}
