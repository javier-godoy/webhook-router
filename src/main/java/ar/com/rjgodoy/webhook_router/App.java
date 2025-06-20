/**
 * Copyright (C) 2024-2025 Roberto Javier Godoy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ar.com.rjgodoy.webhook_router;

import ar.com.rjgodoy.webhook_router.filter.Configuration;
import ar.com.rjgodoy.webhook_router.filter.DirectiveParser;
import ar.com.rjgodoy.webhook_router.filter.ExitActionException;
import java.io.File;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class App
{

  private static String next(byte data[], int pos[]) {
    int count = 0;
    int i = pos[0];
    int offset = i;
    while (i < data.length) {
      switch (data[i++]) {
        case '\r':
          continue;
        case '\n':
          if (++count == 2) {
            pos[0] = i;
            return new String(data, offset, i - 2 - offset);
          } else {
            break;
          }
        default:
          count = 0;
      }

    }
    throw new BufferOverflowException();
  }


  public static void main(String[] args) throws IOException {
    Options options = new Options();

    options.addOption(null, "config", true, "set the path of the configuration file");
    options.addOption(null, "hook", true, "process a directory or file");
    options.addOption(null, "dry", false, "force a dry run");

    CommandLineParser parser = new DefaultParser();
    CommandLine command;
    try {
      command = parser.parse(options, args);
    } catch (org.apache.commons.cli.ParseException exp) {
      System.err.println("Parsing failed. Reason: " + exp.getMessage());
      System.exit(1);
      return;
    }

    Configuration config = null;
    if (command.hasOption("config")) {
      config = parseDirectives(command.getOptionValue("config"));
    }

    boolean dry = false;
    if (command.hasOption("dry")) {
      dry = true;
      DirectiveParser.dry(config);
    }

    if (config == null) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.setOptionComparator(null);
      formatter.printHelp("spool", options);
      System.exit(args.length == 0 ? 0 : 1);
      return;
    }

    if (!command.hasOption("spool")) {
      System.out.println(config);
      return;
    }

    File directory = new File(command.getOptionValue("spool"));
    SpoolManager spool = new SpoolManager(directory);

    for (File file : directory.listFiles()) {
        if (FilenameUtils.getExtension(file.getName()).isEmpty()) {
          spool.enqueue(file);
        }
    }

    List<String> index = spool.readIndex();

    Context context = new Context(spool, config);
    for (String queue : spool.getAllQueues()) {
      for (File file : SpoolManager.sort(spool.discoverPending(queue), index)) {
        process(file, queue, context, dry);
      }
    }

  }


  private static boolean process(File file, String queueName, Context context, boolean dry) {
    WebHook webhook = parseWebHook(context, file);
    if (webhook != null) {
      try {
        context.getRules().call(queueName, webhook);
      } catch (ExitActionException e) {
        // done
      }
      if (!dry && webhook.context.isConsumed()) {
        file.delete();
      }
      return true;
    } else {
      return false;
    }
  }

  private static WebHook parseWebHook(Context context, File file) {
    byte data[];
    try {
      data = FileUtils.readFileToByteArray(file);
    } catch (IOException e) {
      System.err.println("(" + file + ") " + e.getMessage());
      return null;
    }

    int pos[] = new int[1];

    String requestUri, payload;
    List<Header> headers;

    try {
      requestUri = next(data, pos);
      headers = Stream.of(next(data, pos).split("\n")).map(Header::new).toList();
      payload = new String(data, pos[0], data.length - pos[0]);
    } catch (Exception e) {
      System.err.println("(" + file + ") Malformed file: " + e);
      return null;
    }

    String contentType =
        headers.stream().filter(Header.is("Content-Type")).findFirst().get().value();
    if (contentType.equals("application/json")) {
      JSONObject jsonObject;
      try {
        jsonObject = new JSONObject(payload);
      } catch (JSONException e) {
        System.err.println("(" + file + ") Failed to parse payload: " + e.getMessage());
        return null;
      }
      headers = new ArrayList<>(headers);
      return new WebHook(requestUri, headers, jsonObject, file, new Context(context));
    } else {
      System.err.println("(" + file + ") Content type not allowed: contentType");
      return null;
    }

  }

  private static Configuration parseDirectives(String path) throws IOException {
    List<String> lines = FileUtils.readLines(new File(path), Charset.defaultCharset());
    return new DirectiveParser(lines.iterator()).parseConfiguration();
  }
}
