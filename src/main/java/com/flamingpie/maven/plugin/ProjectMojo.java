package com.flamingpie.maven.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Mojo(name = "project")
public class ProjectMojo extends AbstractMojo
{
    private final VelocityEngine velocityEngine = new VelocityEngine();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // Парсим pom.xml с параметрами для плагина (по сути это параметры конфигурации сразу будут здесь доступны в виде полей)
        initVelocity();
        // Создаем директорию .idea если она не создана
        createIdeaProjectDirectory();
        // Добавляем gitignore стандартный если его нет
        createFileIfNotExists("files/.gitignore", "./.idea/.gitignore");

       Map<String, Object> vcsContext = new HashMap<>();
       vcsContext.put("vcs", "svn");

       createFileIfNotExists(writeToTemplate("templates/vcs.vm", vcsContext), "./.idea/vcs.xml");

        // Тут развилка, если нужно полностью пересоздать, то удаляем все файлы и пересоздаем по шаблону

        // Иначе парсим существующие файлы чтобы в них обновить ноды, если файла нет, то создаем по шаблону
    }

    private void initVelocity() throws MojoExecutionException
    {
        try {
            Properties properties = new Properties();
            properties.load(getClass().getClassLoader().getResourceAsStream("properties/velocity.properties"));
            velocityEngine.init(properties);
        } catch (IOException e) {
            throw new MojoExecutionException(e);
        }
    }

    private void createIdeaProjectDirectory() throws MojoExecutionException
    {
        Path ideaDirectoryPath = Path.of("./.idea");
        if (!Files.exists(ideaDirectoryPath))
            try {
                Files.createDirectory(ideaDirectoryPath);
            } catch (IOException e) {
                throw new MojoExecutionException(e);
            }
    }

    private void createFileIfNotExists(String resourcePath, String path) throws MojoExecutionException
    {
        createFileIfNotExists(getClass().getClassLoader().getResourceAsStream(resourcePath), path);
    }

    private void createFileIfNotExists(InputStream inputStream, String stringPath) throws MojoExecutionException
    {
        Path path = Path.of(stringPath);

        if (inputStream == null)
            throw new MojoExecutionException("File '"+ path.getFileName() +"' isn't found!");

        if (Files.exists(path))
            return;

        try (inputStream) {
            Files.copy(inputStream, path);
        } catch (IOException e) {
            throw new MojoExecutionException(e);
        }
    }

    private void createFileIfNotExists(StringWriter stringWriter, String stringPath) throws MojoExecutionException
    {
        Path path = Path.of(stringPath);

        if (stringWriter == null)
            throw new MojoExecutionException("StringWriter is null!");

        if (Files.exists(path))
            return;

        try (BufferedWriter bw = Files.newBufferedWriter(path)) {
            bw.write(stringWriter.toString());
        } catch (IOException e) {
            throw new MojoExecutionException(e);
        }
    }

    private StringWriter writeToTemplate(String templatePath, Map<String, Object> context)
    {
        Template template = velocityEngine.getTemplate(templatePath);
        VelocityContext velocityContext = new VelocityContext(context);

        StringWriter writer =  new StringWriter();
        template.merge(velocityContext, writer);
        return writer;
    }

}
