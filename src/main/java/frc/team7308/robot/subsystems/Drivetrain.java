﻿package frc.team7308.robot.subsystems;

import edu.wpi.first.wpilibj.SpeedControllerGroup;
import edu.wpi.first.wpilibj.Spark;

import frc.team7308.robot.ControlLoop;
import frc.team7308.robot.DriverStation;
import frc.team7308.robot.subsystems.Subsystem;

public class Drivetrain extends Subsystem {
    private SpeedControllerGroup left;
    private SpeedControllerGroup right;
    private double leftSpeed;
    private double rightSpeed;

    private static double mQuickStopAccumulator;
    private static final double kStickDeadband = 0.02;
    private static final double kWheelDeadband = 0.02;
    private static final double kTurnSensitivity = 1.0;

    public static final double kDefaultQuickStopThreshold = 0.2;
    public static final double kDefaultQuickStopAlpha = 0.1;

    private double m_quickStopThreshold = kDefaultQuickStopThreshold;
    private double m_quickStopAlpha = kDefaultQuickStopAlpha;
    private double m_quickStopAccumulator;
    private double m_rightSideInvertMultiplier = -1.0;

    public final ControlLoop controlLoop = new ControlLoop() {
        @Override
        public void loopPeriodic() {
            WheelDrive(DriverStation.driveThrottle, DriverStation.driveRotation, DriverStation.quickTurn);
            left.set(leftSpeed);
            right.set(rightSpeed);
        }
    };

    public Drivetrain() {
        Spark frontLeft = new Spark(0);
        Spark backLeft = new Spark(1);
        Spark frontRight = new Spark(2);
        Spark backRight = new Spark(3);

        this.left = new SpeedControllerGroup(frontLeft, backLeft);
        this.right = new SpeedControllerGroup(frontRight, backRight);

        controlLoop.start();
    }

    public void ArcadeDrive(double movement, double rotation) {
        this.leftSpeed = rotation + movement;
        this.rightSpeed = rotation - movement;
    }

    public void TankDrive(double leftSpeed, double rightSpeed) {
        this.leftSpeed = leftSpeed;
        this.rightSpeed = rightSpeed;
    }

    public void WheelDrive(double throttle, double rotation, boolean quickTurn) {
        double xSpeed = applyDeadzone(throttle, kStickDeadband);
        double zRotation = applyDeadzone(rotation, kWheelDeadband);

        double angularPower;
        boolean overPower;

        if (quickTurn) {
            if (Math.abs(xSpeed) < m_quickStopThreshold) {
                m_quickStopAccumulator = (1 - m_quickStopAlpha) * m_quickStopAccumulator
                    + m_quickStopAlpha * zRotation * 2;
            }
            overPower = true;
            angularPower = zRotation;
        } else {
            overPower = false;
            angularPower = Math.abs(xSpeed) * zRotation - m_quickStopAccumulator;

            if (m_quickStopAccumulator > 1) {
                m_quickStopAccumulator -= 1;
            } else if (m_quickStopAccumulator < -1) {
                m_quickStopAccumulator += 1;
            } else {
                m_quickStopAccumulator = 0.0;
            }
        }

        double leftMotorOutput = xSpeed + angularPower;
        double rightMotorOutput = xSpeed - angularPower;

        // If rotation is overpowered, reduce both outputs to within acceptable range
        if (overPower) {
            if (leftMotorOutput > 1.0) {
                rightMotorOutput -= leftMotorOutput - 1.0;
                leftMotorOutput = 1.0;
            } else if (rightMotorOutput > 1.0) {
                leftMotorOutput -= rightMotorOutput - 1.0;
                rightMotorOutput = 1.0;
            } else if (leftMotorOutput < -1.0) {
                rightMotorOutput -= leftMotorOutput + 1.0;
                leftMotorOutput = -1.0;
            } else if (rightMotorOutput < -1.0) {
                leftMotorOutput -= rightMotorOutput + 1.0;
                rightMotorOutput = -1.0;
            }
        }

        // Normalize the wheel speeds
        double maxMagnitude = Math.max(Math.abs(leftMotorOutput), Math.abs(rightMotorOutput));
        if (maxMagnitude > 1.0) {
            leftMotorOutput /= maxMagnitude;
            rightMotorOutput /= maxMagnitude;
        }

        leftSpeed = leftMotorOutput * 1.0;
        rightSpeed = rightMotorOutput * 1.0 * m_rightSideInvertMultiplier;
    }

    public double applyDeadzone(double val, double deadband) {
        return (Math.abs(val) > Math.abs(deadband)) ? val : 0.0;
    }
}