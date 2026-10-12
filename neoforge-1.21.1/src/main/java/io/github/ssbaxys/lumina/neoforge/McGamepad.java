package io.github.ssbaxys.lumina.neoforge;

import io.github.ssbaxys.lumina.input.Gamepad;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;
import org.lwjgl.system.MemoryStack;

/**
 * Опрос геймпада через GLFW (раскладка SDL, как у XInput): первый подключённый джойстик, у которого
 * GLFW знает раскладку геймпада. Номера кнопок и осей совпадают с {@link Gamepad}.
 */
final class McGamepad {
    private McGamepad() {
    }

    static Gamepad.State poll() {
        for (int jid = GLFW.GLFW_JOYSTICK_1; jid <= GLFW.GLFW_JOYSTICK_LAST; jid++) {
            if (!GLFW.glfwJoystickPresent(jid) || !GLFW.glfwJoystickIsGamepad(jid)) {
                continue;
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                GLFWGamepadState state = GLFWGamepadState.malloc(stack);
                if (!GLFW.glfwGetGamepadState(jid, state)) {
                    continue;
                }
                boolean[] buttons = new boolean[Gamepad.BUTTON_COUNT];
                for (int i = 0; i < buttons.length; i++) {
                    buttons[i] = state.buttons(i) == GLFW.GLFW_PRESS;
                }
                float[] axes = new float[Gamepad.AXIS_COUNT];
                for (int i = 0; i < axes.length; i++) {
                    axes[i] = state.axes(i);
                }
                return new Gamepad.State(buttons, axes);
            }
        }
        return null;
    }
}
