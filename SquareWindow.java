import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * LWJGL3 + OpenGL 3.3 立方体，带贴图。
 * 侧面用 1_side.png，底面用 2_bottom.png，顶面用 3_top.png。
 * 鼠标左/右键拖动旋转，滚轮缩放，ESC 退出。
 */
public class SquareWindow {

    private static final int WIDTH  = 800;
    private static final int HEIGHT = 600;

    private static final String VERTEX_SHADER_SRC =
            "#version 330 core\n" +
            "layout (location = 0) in vec3 aPos;\n" +
            "layout (location = 1) in vec2 aTexCoord;\n" +
            "layout (location = 2) in float aTexIndex;\n" +
            "out vec2 vTexCoord;\n" +
            "out float vTexIndex;\n" +
            "uniform mat4 uMVP;\n" +
            "void main() {\n" +
            "    gl_Position = uMVP * vec4(aPos, 1.0);\n" +
            "    vTexCoord = aTexCoord;\n" +
            "    vTexIndex = aTexIndex;\n" +
            "}\n";

    private static final String FRAGMENT_SHADER_SRC =
            "#version 330 core\n" +
            "in vec2 vTexCoord;\n" +
            "in float vTexIndex;\n" +
            "out vec4 FragColor;\n" +
            "uniform sampler2D uTex0;\n" +   // 侧面
            "uniform sampler2D uTex1;\n" +   // 底面
            "uniform sampler2D uTex2;\n" +   // 顶面
            "void main() {\n" +
            "    vec4 c;\n" +
            "    if (vTexIndex < 0.5)      c = texture(uTex0, vTexCoord);\n" +
            "    else if (vTexIndex < 1.5) c = texture(uTex1, vTexCoord);\n" +
            "    else                      c = texture(uTex2, vTexCoord);\n" +
            "    FragColor = c;\n" +
            "}\n";

    /**
     * 每个顶点 6 个 float：x, y, z, u, v, texIndex
     * texIndex: 0 = 侧面, 1 = 底面, 2 = 顶面
     */
    private static final float[] VERTICES = {
            // 前面 z=+0.5（侧面）
            -0.5f, -0.5f,  0.5f,  0f, 0f,  0f,
             0.5f, -0.5f,  0.5f,  1f, 0f,  0f,
             0.5f,  0.5f,  0.5f,  1f, 1f,  0f,
            -0.5f, -0.5f,  0.5f,  0f, 0f,  0f,
             0.5f,  0.5f,  0.5f,  1f, 1f,  0f,
            -0.5f,  0.5f,  0.5f,  0f, 1f,  0f,

            // 后面 z=-0.5（侧面，UV 镜像一下）
             0.5f, -0.5f, -0.5f,  0f, 0f,  0f,
            -0.5f, -0.5f, -0.5f,  1f, 0f,  0f,
            -0.5f,  0.5f, -0.5f,  1f, 1f,  0f,
             0.5f, -0.5f, -0.5f,  0f, 0f,  0f,
            -0.5f,  0.5f, -0.5f,  1f, 1f,  0f,
             0.5f,  0.5f, -0.5f,  0f, 1f,  0f,

            // 左面 x=-0.5（侧面）
            -0.5f, -0.5f, -0.5f,  0f, 0f,  0f,
            -0.5f, -0.5f,  0.5f,  1f, 0f,  0f,
            -0.5f,  0.5f,  0.5f,  1f, 1f,  0f,
            -0.5f, -0.5f, -0.5f,  0f, 0f,  0f,
            -0.5f,  0.5f,  0.5f,  1f, 1f,  0f,
            -0.5f,  0.5f, -0.5f,  0f, 1f,  0f,

            // 右面 x=+0.5（侧面）
             0.5f, -0.5f,  0.5f,  0f, 0f,  0f,
             0.5f, -0.5f, -0.5f,  1f, 0f,  0f,
             0.5f,  0.5f, -0.5f,  1f, 1f,  0f,
             0.5f, -0.5f,  0.5f,  0f, 0f,  0f,
             0.5f,  0.5f, -0.5f,  1f, 1f,  0f,
             0.5f,  0.5f,  0.5f,  0f, 1f,  0f,

            // 顶面 y=+0.5（顶面贴图）
            -0.5f,  0.5f,  0.5f,  0f, 0f,  2f,
             0.5f,  0.5f,  0.5f,  1f, 0f,  2f,
             0.5f,  0.5f, -0.5f,  1f, 1f,  2f,
            -0.5f,  0.5f,  0.5f,  0f, 0f,  2f,
             0.5f,  0.5f, -0.5f,  1f, 1f,  2f,
            -0.5f,  0.5f, -0.5f,  0f, 1f,  2f,

            // 底面 y=-0.5（底面贴图）
            -0.5f, -0.5f, -0.5f,  0f, 0f,  1f,
             0.5f, -0.5f, -0.5f,  1f, 0f,  1f,
             0.5f, -0.5f,  0.5f,  1f, 1f,  1f,
            -0.5f, -0.5f, -0.5f,  0f, 0f,  1f,
             0.5f, -0.5f,  0.5f,  1f, 1f,  1f,
            -0.5f, -0.5f,  0.5f,  0f, 1f,  1f
    };

    private static final int FLOATS_PER_VERTEX = 6;
    private static final int VERTEX_COUNT = VERTICES.length / FLOATS_PER_VERTEX;

    // ---------- 交互状态 ----------
    private static float rotX = 0.3f;
    private static float rotY = 0.4f;
    private static float distance = 3.0f;

    private static boolean leftDown  = false;
    private static boolean rightDown = false;
    private static boolean dragging  = false;
    private static double lastMouseX = 0.0;
    private static double lastMouseY = 0.0;

    private static GLFWMouseButtonCallback mouseButtonCallback;
    private static GLFWCursorPosCallback   cursorPosCallback;
    private static GLFWScrollCallback      scrollCallback;
    private static GLFWKeyCallback         keyCallback;

    public static void main(String[] args) {
        GLFWErrorCallback errorCallback = GLFWErrorCallback.createPrint(System.err);
        errorCallback.set();
        if (!glfwInit()) {
            throw new IllegalStateException("GLFW 初始化失败");
        }

        long window = NULL;
        try {
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);

            window = glfwCreateWindow(WIDTH, HEIGHT, "3D TNT", NULL, NULL);
            if (window == NULL) {
                throw new RuntimeException("创建窗口失败");
            }

            glfwMakeContextCurrent(window);
            glfwSwapInterval(1);
            GL.createCapabilities();
            glfwShowWindow(window);

            setupInputCallbacks(window);

            renderLoop(window);

        } finally {
            if (mouseButtonCallback != null) mouseButtonCallback.free();
            if (cursorPosCallback   != null) cursorPosCallback.free();
            if (scrollCallback      != null) scrollCallback.free();
            if (keyCallback         != null) keyCallback.free();

            if (window != NULL) {
                glfwDestroyWindow(window);
            }
            glfwTerminate();
            GLFWErrorCallback cb = glfwSetErrorCallback(null);
            if (cb != null) cb.free();
        }
    }

    private static void setupInputCallbacks(long window) {
        mouseButtonCallback = glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button != GLFW_MOUSE_BUTTON_LEFT && button != GLFW_MOUSE_BUTTON_RIGHT) return;

            boolean wasDragging = dragging;

            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                leftDown = (action == GLFW_PRESS);
            } else {
                rightDown = (action == GLFW_PRESS);
            }
            dragging = leftDown || rightDown;

            if (dragging && !wasDragging) {
                double[] xbuf = new double[1];
                double[] ybuf = new double[1];
                glfwGetCursorPos(win, xbuf, ybuf);
                lastMouseX = xbuf[0];
                lastMouseY = ybuf[0];
            }
        });

        cursorPosCallback = glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
            if (!dragging) return;
            double dx = xpos - lastMouseX;
            double dy = ypos - lastMouseY;
            rotY += (float) dx * 0.01f;
            rotX += (float) dy * 0.01f;
            lastMouseX = xpos;
            lastMouseY = ypos;
        });

        scrollCallback = glfwSetScrollCallback(window, (win, xoffset, yoffset) -> {
            distance -= (float) yoffset * 0.3f;
            if (distance < 1.0f)  distance = 1.0f;
            if (distance > 20.0f) distance = 20.0f;
        });

        keyCallback = glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                glfwSetWindowShouldClose(win, true);
            }
        });
    }

    private static void renderLoop(long window) {
        int program = createProgram();
        int mvpLocation = glGetUniformLocation(program, "uMVP");

        // 加载三张纹理
        STBImage.stbi_set_flip_vertically_on_load(true);
        int texSide   = loadTexture("1_side.png");
        int texBottom = loadTexture("2_bottom.png");
        int texTop    = loadTexture("3_top.png");

        // sampler 单元绑定：uTex0 -> 0, uTex1 -> 1, uTex2 -> 2
        glUseProgram(program);
        glUniform1i(glGetUniformLocation(program, "uTex0"), 0);
        glUniform1i(glGetUniformLocation(program, "uTex1"), 1);
        glUniform1i(glGetUniformLocation(program, "uTex2"), 2);

        // 创建 VAO / VBO
        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer buffer = MemoryUtil.memAllocFloat(VERTICES.length);
        buffer.put(VERTICES).flip();
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(buffer);

        int stride = FLOATS_PER_VERTEX * Float.BYTES;
        // location 0: 位置
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        // location 1: UV
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        // location 2: 纹理索引
        glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, 5 * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);

        glViewport(0, 0, WIDTH, HEIGHT);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);          // 打开背面剔除，看穿内部时更干净
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);

        while (!glfwWindowShouldClose(window)) {
            glClearColor(0.10f, 0.11f, 0.15f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            float aspect = (float) WIDTH / HEIGHT;
            float[] projection = perspective(45.0f, aspect, 0.1f, 100.0f);
            float[] view       = translate(0f, 0f, -distance);
            float[] model      = multiply(rotateX(rotX), rotateY(rotY));
            float[] mvp        = multiply(multiply(projection, view), model);

            glUseProgram(program);
            glUniformMatrix4fv(mvpLocation, false, mvp);

            // 绑定三张纹理到对应单元
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, texSide);
            glActiveTexture(GL_TEXTURE1);
            glBindTexture(GL_TEXTURE_2D, texBottom);
            glActiveTexture(GL_TEXTURE2);
            glBindTexture(GL_TEXTURE_2D, texTop);

            glBindVertexArray(vao);
            glDrawArrays(GL_TRIANGLES, 0, VERTEX_COUNT);
            glBindVertexArray(0);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }

        glDeleteTextures(new int[]{texSide, texBottom, texTop});
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        glDeleteProgram(program);
    }

    // ---------------- 纹理加载 ----------------

private static int loadTexture(String path) {
    IntBuffer w = MemoryUtil.memAllocInt(1);
    IntBuffer h = MemoryUtil.memAllocInt(1);
    IntBuffer comp = MemoryUtil.memAllocInt(1);

    ByteBuffer data = STBImage.stbi_load(path, w, h, comp, 4);
    if (data == null) {
        String reason = STBImage.stbi_failure_reason();
        MemoryUtil.memFree(w);
        MemoryUtil.memFree(h);
        MemoryUtil.memFree(comp);
        throw new RuntimeException("加载纹理失败: " + path + " -> " + reason);
    }

    int width  = w.get(0);
    int height = h.get(0);

    int texId = glGenTextures();
    glBindTexture(GL_TEXTURE_2D, texId);

    // 用 CLAMP_TO_EDGE，避免边缘出现重复像素
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    // 最近邻：不做插值，像素分明
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0,
            GL_RGBA, GL_UNSIGNED_BYTE, data);

    STBImage.stbi_image_free(data);
    MemoryUtil.memFree(w);
    MemoryUtil.memFree(h);
    MemoryUtil.memFree(comp);

    return texId;
}

    // ---------------- 着色器工具 ----------------

    private static int createProgram() {
        int vertexShader = compileShader(GL_VERTEX_SHADER, VERTEX_SHADER_SRC);
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, FRAGMENT_SHADER_SRC);

        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);

        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(program);
            glDeleteProgram(program);
            throw new RuntimeException("着色器程序链接失败:\n" + log);
        }

        glDetachShader(program, vertexShader);
        glDetachShader(program, fragmentShader);
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        return program;
    }

    private static int compileShader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new RuntimeException("着色器编译失败:\n" + log);
        }
        return shader;
    }

    // ---------------- 矩阵工具 ----------------

    private static float[] perspective(float fovyDeg, float aspect, float near, float far) {
        float f = (float) (1.0 / Math.tan(Math.toRadians(fovyDeg) / 2.0));
        float[] m = new float[16];
        m[0]  = f / aspect;
        m[5]  = f;
        m[10] = (far + near) / (near - far);
        m[11] = -1.0f;
        m[14] = (2.0f * far * near) / (near - far);
        return m;
    }

    private static float[] translate(float tx, float ty, float tz) {
        return new float[] {
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                tx, ty, tz, 1f
        };
    }

    private static float[] rotateX(float angle) {
        float c = (float) Math.cos(angle);
        float s = (float) Math.sin(angle);
        return new float[] {
                1f, 0f, 0f, 0f,
                0f, c,  s,  0f,
                0f, -s, c,  0f,
                0f, 0f, 0f, 1f
        };
    }

    private static float[] rotateY(float angle) {
        float c = (float) Math.cos(angle);
        float s = (float) Math.sin(angle);
        return new float[] {
                c,  0f, -s, 0f,
                0f, 1f, 0f, 0f,
                s,  0f, c,  0f,
                0f, 0f, 0f, 1f
        };
    }

    private static float[] multiply(float[] a, float[] b) {
        float[] r = new float[16];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                float sum = 0f;
                for (int k = 0; k < 4; k++) {
                    sum += a[k * 4 + j] * b[i * 4 + k];
                }
                r[i * 4 + j] = sum;
            }
        }
        return r;
    }
}