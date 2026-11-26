using System;
using System.IO;
using System.Linq;
using Mono.Cecil;

namespace VerifyResourcesInjection
{
    class Program
    {
        static void Main(string[] args)
        {
            string gamePath = @\"I:\\rail rush\\Rail Rush_Data\\Resources\\Assembly-CSharp.dll\";

            var resolver = new DefaultAssemblyResolver();
            resolver.AddSearchDirectory(Path.GetDirectoryName(gamePath));
            resolver.AddSearchDirectory(@\"I:\\rail rush\\Rail Rush_Data\\Managed\");

            var parameters = new ReaderParameters { AssemblyResolver = resolver, ReadSymbols = false };
            var assembly = AssemblyDefinition.ReadAssembly(gamePath, parameters);

            // Check RunnerPreloader.Update
            var runnerPreloader = assembly.MainModule.Types.FirstOrDefault(t => t.Name == \"RunnerPreloader\");
            if (runnerPreloader != null)
            {
                var update = runnerPreloader.Methods.FirstOrDefault(m => m.Name == \"Update\" && !m.IsStatic);
                if (update != null)
                {
                    Console.WriteLine(\"RunnerPreloader.Update - \" + update.Body.Instructions.Count + \" instructions (first 30):\");
                    for (int i = 0; i < Math.Min(30, update.Body.Instructions.Count); i++)
                    {
                        Console.WriteLine(\"  \" + update.Body.Instructions[i]);
                    }
                }
            }

            // Check RunnerInterface.cctor
            var runnerInterface = assembly.MainModule.Types.FirstOrDefault(t => t.Name == \"RunnerInterface\");
            if (runnerInterface != null)
            {
                var cctor = runnerInterface.Methods.FirstOrDefault(m => m.Name == \".cctor\");
                if (cctor != null)
                {
                    Console.WriteLine(\"\\nRunnerInterface.cctor - \" + cctor.Body.Instructions.Count + \" instructions (first 30):\");
                    for (int i = 0; i < Math.Min(30, cctor.Body.Instructions.Count); i++)
                    {
                        Console.WriteLine(\"  \" + cctor.Body.Instructions[i]);
                    }
                }
            }

            // Check UIManager.Awake
            var uiManager = assembly.MainModule.Types.FirstOrDefault(t => t.Name == \"UIManager\");
            if (uiManager != null)
            {
                var awake = uiManager.Methods.FirstOrDefault(m => m.Name == \"Awake\" && !m.IsStatic);
                if (awake != null)
                {
                    Console.WriteLine(\"\\nUIManager.Awake - \" + awake.Body.Instructions.Count + \" instructions (first 30):\");
                    for (int i = 0; i < Math.Min(30, awake.Body.Instructions.Count); i++)
                    {
                        Console.WriteLine(\"  \" + awake.Body.Instructions[i]);
                    }
                }
            }

            // Check ArcadeAllowPlay field
            if (runnerInterface != null)
            {
                var arcadeField = runnerInterface.Fields.FirstOrDefault(f => f.Name == \"ArcadeAllowPlay\");
                if (arcadeField != null)
                    Console.WriteLine(\"\\nRunnerInterface.ArcadeAllowPlay field: FOUND\");
                else
                    Console.WriteLine(\"\\nRunnerInterface.ArcadeAllowPlay field: MISSING\");
            }

            // Check onPlayRelease
            if (runnerInterface != null)
            {
                var onPlayRelease = runnerInterface.Methods.FirstOrDefault(m => m.Name == \"onPlayRelease\" && !m.IsStatic);
                if (onPlayRelease != null)
                {
                    Console.WriteLine(\"\\nRunnerInterface.onPlayRelease - \" + onPlayRelease.Body.Instructions.Count + \" instructions (first 30):\");
                    for (int i = 0; i < Math.Min(30, onPlayRelease.Body.Instructions.Count); i++)
                    {
                        Console.WriteLine(\"  \" + onPlayRelease.Body.Instructions[i]);
                    }
                }
            }

            // Check ShopItem.get_NumOwn
            var shopItem = assembly.MainModule.Types.FirstOrDefault(t => t.Name == \"ShopItem\");
            if (shopItem != null)
            {
                var getNumOwn = shopItem.Methods.FirstOrDefault(m => m.Name == \"get_NumOwn\" && !m.IsStatic);
                if (getNumOwn != null)
                {
                    Console.WriteLine(\"\\nShopItem.get_NumOwn - \" + getNumOwn.Body.Instructions.Count + \" instructions:\");
                    for (int i = 0; i < getNumOwn.Body.Instructions.Count; i++)
                    {
                        Console.WriteLine(\"  \" + getNumOwn.Body.Instructions[i]);
                    }
                }
            }

            // Check ExitToRewardPage -> ExitToStartPage
            if (runnerInterface != null)
            {
                var exitReward = runnerInterface.Methods.FirstOrDefault(m => m.Name == \"ExitToRewardPage\" && !m.IsStatic);
                if (exitReward != null)
                {
                    Console.WriteLine(\"\\nRunnerInterface.ExitToRewardPage - \" + exitReward.Body.Instructions.Count + \" instructions:\");
                    for (int i = 0; i < exitReward.Body.Instructions.Count; i++)
                    {
                        Console.WriteLine(\"  \" + exitReward.Body.Instructions[i]);
                    }
                }
                var exitStart = runnerInterface.Methods.FirstOrDefault(m => m.Name == \"ExitToStartPage\" && !m.IsStatic);
                if (exitStart != null)
                {
                    Console.WriteLine(\"\\nRunnerInterface.ExitToStartPage - \" + exitStart.Body.Instructions.Count + \" instructions:\");
                    for (int i = 0; i < exitStart.Body.Instructions.Count; i++)
                    {
                        Console.WriteLine(\"  \" + exitStart.Body.Instructions[i]);
                    }
                }
            }
        }
    }
}